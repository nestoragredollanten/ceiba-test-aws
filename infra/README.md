# Infraestructura BTG Pactual Fondos API

La documentacion principal y actualizada ya no vive aqui.

## Usa el README principal del proyecto

Consulta:

- `README.md`

Ese archivo contiene ahora el flujo unico y vigente para:

1. desplegar IaC con CloudFormation,
2. asociar OIDC + configurar IRSA,
3. construir y subir la imagen Docker a ECR,
4. desplegar la aplicacion en EKS,
5. usar el script unico `deploy-aws-eks.ps1`.

## Archivos relevantes de infraestructura

- `infra/cloudformation/btg-funds-infra.yaml`
- `infra/k8s/namespace.yaml`
- `infra/k8s/configmap.yaml`
- `infra/k8s/serviceaccount.yaml`
- `infra/k8s/deployment.yaml`
- `infra/k8s/service.yaml`
