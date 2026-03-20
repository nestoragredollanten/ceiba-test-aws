param(
    [Parameter(Mandatory = $true)]
    [string]$VpcId,

    [Parameter(Mandatory = $true)]
    [string]$EksControlPlaneSubnetIds,

    [Parameter(Mandatory = $true)]
    [string]$NodeGroupSubnetIds,

    [Parameter(Mandatory = $true)]
    [string]$AccountId,

    [string]$Region = "us-east-1",
    [string]$Environment = "prod",
    [string]$ClusterName = "btg-funds-eks",
    [string]$StackName = "btg-funds-infra-prod",
    [string]$ImageTag = "v1",
    [string]$Namespace = "ceiba-test",
    [string]$ServiceAccountName = "btg-funds-sa",
    [string]$DeploymentName = "ceiba-test-api",
    [string]$ContainerName = "ceiba-test-api",
    [string]$IrsaRoleArn = "",
    [string]$IrsaRoleName = "btg-funds-irsa-role-prod",
    [string]$IrsaPolicyArn = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Assert-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Comando requerido no encontrado: $Name"
    }
}

function Invoke-External {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,

        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Comando fallo: $FilePath $($Arguments -join ' ')"
    }
}

function Invoke-ExternalCapture {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,

        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [switch]$AllowFailure
    )

    $output = & $FilePath @Arguments 2>&1
    $exitCode = $LASTEXITCODE

    if (-not $AllowFailure -and $exitCode -ne 0) {
        throw "Comando fallo: $FilePath $($Arguments -join ' ')`n$($output -join "`n")"
    }

    return [pscustomobject]@{
        ExitCode = $exitCode
        Output   = ($output -join "`n")
    }
}

function Invoke-AwsJson {
    param([string[]]$Arguments)

    $result = Invoke-ExternalCapture -FilePath "aws" -Arguments $Arguments
    if ([string]::IsNullOrWhiteSpace($result.Output)) {
        return $null
    }
    return $result.Output | ConvertFrom-Json
}

function Ensure-OidcProvider {
    param(
        [string]$IssuerUrl,
        [string]$Thumbprint
    )

    $issuerHost = $IssuerUrl.Replace("https://", "")
    $providers = Invoke-AwsJson -Arguments @("iam", "list-open-id-connect-providers", "--output", "json")

    foreach ($provider in $providers.OpenIDConnectProviderList) {
        $details = Invoke-AwsJson -Arguments @("iam", "get-open-id-connect-provider", "--open-id-connect-provider-arn", $provider.Arn, "--output", "json")
        if ($details.Url -eq $issuerHost) {
            return $provider.Arn
        }
    }

    Write-Host "== Creando proveedor OIDC en IAM ==" -ForegroundColor Cyan
    $created = Invoke-AwsJson -Arguments @(
        "iam", "create-open-id-connect-provider",
        "--url", $IssuerUrl,
        "--client-id-list", "sts.amazonaws.com",
        "--thumbprint-list", $Thumbprint,
        "--output", "json"
    )
    return $created.OpenIDConnectProviderArn
}

function Ensure-IrsaRole {
    param(
        [string]$RoleName,
        [string]$RoleArn,
        [string]$PolicyArn,
        [string]$ProviderArn,
        [string]$IssuerHost,
        [string]$Namespace,
        [string]$ServiceAccountName
    )

    $trustPolicyPath = Join-Path $PWD "trust-policy.generated.json"
    $trustPolicy = @"
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "$ProviderArn"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "${IssuerHost}:aud": "sts.amazonaws.com",
          "${IssuerHost}:sub": "system:serviceaccount:${Namespace}:${ServiceAccountName}"
        }
      }
    }
  ]
}
"@
    Set-Content -Path $trustPolicyPath -Value $trustPolicy -Encoding ascii

    $existingRole = Invoke-ExternalCapture -FilePath "aws" -Arguments @("iam", "get-role", "--role-name", $RoleName, "--output", "json") -AllowFailure
    if ($existingRole.ExitCode -eq 0) {
        Write-Host "== Actualizando trust policy del rol IRSA existente ==" -ForegroundColor Cyan
        Invoke-External -FilePath "aws" -Arguments @("iam", "update-assume-role-policy", "--role-name", $RoleName, "--policy-document", "file://$trustPolicyPath")
    }
    else {
        Write-Host "== Creando rol IRSA ==" -ForegroundColor Cyan
        Invoke-External -FilePath "aws" -Arguments @("iam", "create-role", "--role-name", $RoleName, "--assume-role-policy-document", "file://$trustPolicyPath")
    }

    $attachedPolicies = Invoke-AwsJson -Arguments @("iam", "list-attached-role-policies", "--role-name", $RoleName, "--output", "json")
    $alreadyAttached = $false
    foreach ($policy in $attachedPolicies.AttachedPolicies) {
        if ($policy.PolicyArn -eq $PolicyArn) {
            $alreadyAttached = $true
            break
        }
    }

    if (-not $alreadyAttached) {
        Write-Host "== Adjuntando policy al rol IRSA ==" -ForegroundColor Cyan
        Invoke-External -FilePath "aws" -Arguments @("iam", "attach-role-policy", "--role-name", $RoleName, "--policy-arn", $PolicyArn)
    }

    Remove-Item $trustPolicyPath -ErrorAction SilentlyContinue
    return $RoleArn
}

Write-Host "== Verificando prerequisitos ==" -ForegroundColor Cyan
Assert-Command "aws"
Assert-Command "docker"
Assert-Command "kubectl"

$repoUri = "$AccountId.dkr.ecr.$Region.amazonaws.com/btg-funds-api"
$localImage = "ceiba-test-api:$ImageTag"
$templatePath = "infra/cloudformation/btg-funds-infra.yaml"
$clusterFullName = "$ClusterName-$Environment"
$effectiveIrsaPolicyArn = if ($IrsaPolicyArn) { $IrsaPolicyArn } else { "arn:aws:iam::$AccountId:policy/btg-funds-app-policy-$Environment" }
$effectiveIrsaRoleArn = if ($IrsaRoleArn) { $IrsaRoleArn } else { "arn:aws:iam::$AccountId:role/$IrsaRoleName" }

Write-Host "== Validando identidad AWS ==" -ForegroundColor Cyan
aws sts get-caller-identity | Out-Null

Write-Host "== Desplegando IaC con CloudFormation ==" -ForegroundColor Cyan
aws cloudformation deploy `
  --region $Region `
  --stack-name $StackName `
  --template-file $templatePath `
  --capabilities CAPABILITY_NAMED_IAM `
  --parameter-overrides `
    Environment=$Environment `
    ClusterName=$ClusterName `
    VpcId=$VpcId `
    EksControlPlaneSubnetIds=$EksControlPlaneSubnetIds `
    NodeGroupSubnetIds=$NodeGroupSubnetIds

Write-Host "== Construyendo imagen Docker ==" -ForegroundColor Cyan
docker build -t $localImage .

Write-Host "== Login en ECR ==" -ForegroundColor Cyan
aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin "$AccountId.dkr.ecr.$Region.amazonaws.com"

Write-Host "== Publicando imagen en ECR ==" -ForegroundColor Cyan
docker tag $localImage "${repoUri}:${ImageTag}"
docker push "${repoUri}:${ImageTag}"

Write-Host "== Configurando kubeconfig para EKS ==" -ForegroundColor Cyan
aws eks update-kubeconfig --region $Region --name $clusterFullName

Write-Host "== Asegurando OIDC + IRSA ==" -ForegroundColor Cyan
$oidcIssuer = (& aws eks describe-cluster --region $Region --name $clusterFullName --query "cluster.identity.oidc.issuer" --output text)
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($oidcIssuer)) {
    throw "No fue posible obtener el issuer OIDC del cluster $clusterFullName"
}
$oidcHost = $oidcIssuer.Replace("https://", "")
$oidcProviderArn = Ensure-OidcProvider -IssuerUrl $oidcIssuer -Thumbprint "9e99a48a9960b14926bb7f3b02e22da0ecd6c8d1"
Ensure-IrsaRole -RoleName $IrsaRoleName -RoleArn $effectiveIrsaRoleArn -PolicyArn $effectiveIrsaPolicyArn -ProviderArn $oidcProviderArn -IssuerHost $oidcHost -Namespace $Namespace -ServiceAccountName $ServiceAccountName | Out-Null

Write-Host "== Aplicando manifiestos Kubernetes ==" -ForegroundColor Cyan
kubectl apply -f infra/k8s/namespace.yaml
kubectl apply -f infra/k8s/configmap.yaml
kubectl apply -f infra/k8s/serviceaccount.yaml
kubectl apply -f infra/k8s/deployment.yaml
kubectl apply -f infra/k8s/service.yaml

Write-Host "== Anotando ServiceAccount con rol IRSA ==" -ForegroundColor Cyan
kubectl annotate serviceaccount $ServiceAccountName -n $Namespace "eks.amazonaws.com/role-arn=$effectiveIrsaRoleArn" --overwrite

Write-Host "== Actualizando imagen del deployment ==" -ForegroundColor Cyan
kubectl set image deployment/$DeploymentName $ContainerName="${repoUri}:${ImageTag}" -n $Namespace
kubectl rollout status deployment/$DeploymentName -n $Namespace

Write-Host "== Validacion final ==" -ForegroundColor Green
kubectl get pods -n $Namespace
kubectl get svc -n $Namespace
kubectl get deployment $DeploymentName -n $Namespace -o jsonpath="{.spec.template.spec.containers[0].image}"
Write-Host ""
Write-Host "Rol IRSA configurado: $effectiveIrsaRoleArn"
Write-Host "Despliegue completado." -ForegroundColor Green

