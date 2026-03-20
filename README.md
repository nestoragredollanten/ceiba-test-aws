# ceiba-test - Guia unica de despliegue (IaC + ECR + EKS)

Este README centraliza la documentacion esencial para desplegar la API en AWS usando:

- CloudFormation (IaC)
- ECR (imagen Docker)
- EKS (Kubernetes)

Tambien se incluye un script unico `deploy-aws-eks.ps1` para automatizar el flujo.

> El script `deploy-aws-eks.ps1` ya automatiza tambien la configuracion OIDC + IRSA (creacion/reuso del OIDC provider, rol IRSA, adjuncion de policy y anotacion del `ServiceAccount`).

## 1. Prerrequisitos

- AWS CLI v2 autenticado con permisos para CloudFormation, IAM, ECR, EKS, EC2, DynamoDB y SNS.
- Docker Desktop operativo.
- `kubectl` instalado.
- `eksctl` instalado (opcional, solo si quieres hacer OIDC/IRSA manualmente por fuera del script).
- PowerShell (Windows).
- Acceso a una VPC en `us-east-1` con subnets validas para EKS.

Verificacion rapida:

```powershell
aws sts get-caller-identity
aws --version
kubectl version --client
docker --version
```

Instalacion rapida de `eksctl` en Windows (opcional):

```powershell
winget install Weaveworks.Eksctl
```

## 2. Variables que debes definir

- `VpcId`: VPC donde crear EKS.
- `EksControlPlaneSubnetIds`: minimo 2 subnets en AZ distintas soportadas por EKS para control plane.
- `NodeGroupSubnetIds`: subnets para nodos (pueden ser las mismas).
- `AccountId`: cuenta AWS para URI de ECR.

Para listar subnets y AZ:

```powershell
aws ec2 describe-subnets --filters Name=vpc-id,Values=vpc-xxxxxxxx --query "Subnets[*].[SubnetId,AvailabilityZone,CidrBlock]" --output table
```

## 3. Paso a paso manual

### 3.1 Desplegar IaC (CloudFormation)

Template unificado: `infra/cloudformation/btg-funds-infra.yaml`

```powershell
aws cloudformation deploy `
  --region us-east-1 `
  --stack-name btg-funds-infra-prod `
  --template-file infra/cloudformation/btg-funds-infra.yaml `
  --capabilities CAPABILITY_NAMED_IAM `
  --parameter-overrides `
    Environment=prod `
    ClusterName=btg-funds-eks `
    VpcId=vpc-xxxxxxxx `
    EksControlPlaneSubnetIds=subnet-aaa,subnet-bbb `
    NodeGroupSubnetIds=subnet-aaa,subnet-bbb,subnet-ccc
```

Obtener outputs del stack:

```powershell
aws cloudformation describe-stacks --region us-east-1 --stack-name btg-funds-infra-prod --query "Stacks[0].Outputs" --output table
```

### 3.1.1 Configurar OIDC + IRSA para que los pods accedan a DynamoDB/SNS (manual, opcional)

Si ves un error como este en los logs del pod:

```text
No OpenIDConnect provider found in your account for https://oidc.eks...
```

la causa es que el pod esta intentando usar Web Identity (IRSA), pero falta:

1. asociar el proveedor OIDC del cluster en IAM,
2. usar un rol IRSA dedicado para el `ServiceAccount`.

#### A. Verificar issuer OIDC del cluster

```powershell
aws eks describe-cluster --region us-east-1 --name <CLUSTER_NAME> --query "cluster.identity.oidc.issuer" --output text
aws iam list-open-id-connect-providers
```

#### B. Asociar el proveedor OIDC del cluster

> Esta seccion es solo de referencia si quieres hacer IRSA manualmente. Si usas `deploy-aws-eks.ps1`, este paso ya queda automatizado.

```powershell
eksctl utils associate-iam-oidc-provider --cluster <CLUSTER_NAME> --region us-east-1 --approve
```

Alternativa manual sin `eksctl`:

```powershell
$Issuer = aws eks describe-cluster --region us-east-1 --name <CLUSTER_NAME> --query "cluster.identity.oidc.issuer" --output text
$OidcHost = $Issuer.Replace("https://", "")
aws iam create-open-id-connect-provider --url $Issuer --client-id-list sts.amazonaws.com --thumbprint-list 9e99a48a9960b14926bb7f3b02e22da0ecd6c8d1
```

> Si el proveedor ya existe, AWS respondera un error indicando duplicado. En ese caso puedes continuar con el rol IRSA.

#### C. Crear trust policy para un rol IRSA dedicado

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/oidc.eks.us-east-1.amazonaws.com/id/<OIDC_ID>"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "oidc.eks.us-east-1.amazonaws.com/id/<OIDC_ID>:aud": "sts.amazonaws.com",
          "oidc.eks.us-east-1.amazonaws.com/id/<OIDC_ID>:sub": "system:serviceaccount:ceiba-test:btg-funds-sa"
        }
      }
    }
  ]
}
```

#### D. Crear el rol y adjuntar permisos

```powershell
aws iam create-role --role-name <IRSA_ROLE_NAME> --assume-role-policy-document file://trust-policy.json
aws iam attach-role-policy --role-name <IRSA_ROLE_NAME> --policy-arn arn:aws:iam::<ACCOUNT_ID>:policy/<IRSA_POLICY_NAME>
```

#### E. Anotar el `ServiceAccount` con el rol IRSA

```powershell
kubectl annotate serviceaccount btg-funds-sa -n ceiba-test eks.amazonaws.com/role-arn=arn:aws:iam::<ACCOUNT_ID>:role/<IRSA_ROLE_NAME> --overwrite
kubectl rollout restart deployment/ceiba-test-api -n ceiba-test
kubectl rollout status deployment/ceiba-test-api -n ceiba-test
```

### 3.2 Construir y subir imagen Docker a ECR

```powershell
$Region = "us-east-1"
$AccountId = "<ACCOUNT_ID>"
$Tag = "v1"
$LocalImage = "ceiba-test-api:$Tag"
$Repo = "$AccountId.dkr.ecr.$Region.amazonaws.com/btg-funds-api"

docker build -t $LocalImage .
aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin "$AccountId.dkr.ecr.$Region.amazonaws.com"
docker tag $LocalImage "${Repo}:${Tag}"
docker push "${Repo}:${Tag}"
```

### 3.3 Desplegar en EKS

Configurar `kubectl`:

```powershell
aws eks update-kubeconfig --region us-east-1 --name <CLUSTER_NAME>
kubectl get nodes
```

Aplicar manifiestos:

```powershell
kubectl apply -f infra/k8s/namespace.yaml
kubectl apply -f infra/k8s/configmap.yaml
kubectl apply -f infra/k8s/serviceaccount.yaml
kubectl apply -f infra/k8s/deployment.yaml
kubectl apply -f infra/k8s/service.yaml
```

> Si haces el despliegue manual, reemplaza el ARN placeholder de `serviceaccount.yaml` por el rol IRSA real. Si usas el script, la anotacion se sobreescribe automaticamente.

Actualizar imagen del deployment:

```powershell
kubectl set image deployment/ceiba-test-api ceiba-test-api=<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btg-funds-api:<IMAGE_TAG> -n ceiba-test
kubectl rollout status deployment/ceiba-test-api -n ceiba-test
```

Verificar servicio y endpoint:

```powershell
kubectl get pods -n ceiba-test
kubectl get svc -n ceiba-test
```

## 4. Automatizacion con un solo script `.ps1`

Script unico: `deploy-aws-eks.ps1`

Ejemplo de uso:

```powershell
.\deploy-aws-eks.ps1 `
  -VpcId "vpc-xxxxxxxx" `
  -EksControlPlaneSubnetIds "subnet-aaa,subnet-bbb" `
  -NodeGroupSubnetIds "subnet-aaa,subnet-bbb,subnet-ccc" `
  -AccountId "<ACCOUNT_ID>" `
  -ImageTag "v1"
```

Opcionalmente puedes personalizar el rol/policy IRSA:

```powershell
.\deploy-aws-eks.ps1 `
  -VpcId "vpc-xxxxxxxx" `
  -EksControlPlaneSubnetIds "subnet-aaa,subnet-bbb" `
  -NodeGroupSubnetIds "subnet-aaa,subnet-bbb,subnet-ccc" `
  -AccountId "<ACCOUNT_ID>" `
  -IrsaRoleName "<IRSA_ROLE_NAME>" `
  -IrsaPolicyArn "arn:aws:iam::<ACCOUNT_ID>:policy/<IRSA_POLICY_NAME>" `
  -ImageTag "v1"
```

El script ejecuta:

1. `cloudformation deploy` del template unificado.
2. `docker build/tag/push` a ECR.
3. `aws eks update-kubeconfig`.
4. detecta el issuer OIDC del cluster y crea/reutiliza el OIDC provider en IAM.
5. crea o actualiza el rol IRSA y adjunta la policy de la app.
6. `kubectl apply` de manifiestos.
7. anota el `ServiceAccount` con el rol IRSA.
8. `kubectl set image` + `rollout status`.

## 5. Pruebas rapidas API (una vez desplegada)

```powershell
curl.exe -i "http://<EXTERNAL-HOST>/actuator/health"
curl.exe -i -u cliente:cliente123 "http://<EXTERNAL-HOST>/api/v1/funds"
```

## 6. Troubleshooting minimo

- Ver eventos de CloudFormation:

```powershell
aws cloudformation describe-stack-events --region us-east-1 --stack-name btg-funds-infra-prod --output table
```

- Ver logs del pod:

```powershell
kubectl logs -n ceiba-test deployment/ceiba-test-api --tail=200
```

- Ver imagen real desplegada:

```powershell
kubectl get deployment ceiba-test-api -n ceiba-test -o jsonpath="{.spec.template.spec.containers[0].image}"
```

- Verificar variables AWS en pod:

```powershell
kubectl exec -n ceiba-test deploy/ceiba-test-api -- printenv | findstr AWS_
```


