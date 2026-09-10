# Pedidos360 — Guía de Contribución

Repositorio: https://github.com/victor99a/Cloud-Native

## División del trabajo (3 integrantes)

Cada integrante trabaja en una **rama independiente** y sube sus commits. Las áreas **no se solapan**, por lo que no habrá conflictos de merge.

| Integrante | Área | Rama | Carpeta / Archivos |
|------------|------|------|--------------------|
| **Integrante 1** — Backend | BFF Spring Boot | `feature/backend-bff` | `pedidos360-bff/**` |
| **Integrante 2** — Frontend Core | Auth MSAL + core | `feature/frontend-core` | `pedidos360-frontend/src/app/core/**`, `app.config.ts`, `app.routes.ts`, `app.component.ts`, `environments/**` |
| **Integrante 3** — Frontend UI | Features + Shared | `feature/frontend-ui` | `pedidos360-frontend/src/app/features/**`, `pedidos360-frontend/src/app/shared/**` |

## Detalle por integrante

### Integrante 1 — Backend BFF (`pedidos360-bff`)
- `security/`: `JwtAuthenticationConverter`, `AzureAdRolesConverter`, `SecurityUtils`
- `config/`: `SecurityConfig`, `CorsConfig`, `RestClientConfig`, `JpaAuditingConfig`
- `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/`
- `resources/application.yml` (Azure AD + PostgreSQL)

### Integrante 2 — Frontend Core + Auth (`pedidos360-frontend`)
- `core/auth/`: `msal-config.ts`, `auth.service.ts`, `guards/auth.guard.ts`, `interceptors/msal.interceptor.ts`
- `core/interceptors/`: `http-error`, `auth-token`, `loading`
- `core/services/`: `api.service.ts`, `error-handler.service.ts`, `logger.service.ts`
- `app.config.ts`, `app.routes.ts`, `app.component.ts`, `environments/`

### Integrante 3 — Frontend UI (`pedidos360-frontend`)
- `features/pedidos/`, `features/productos/`, `features/dashboard/` (pages, components, services, models, routes)
- `shared/`: `components/` (button, modal, loader), `material/`, `pipes/`, `directives/`, `utils/`, `constants/`

## Flujo de trabajo (Git)

### Opción A — Colaboradores (recomendada para curso)

El dueño del repo agrega a los otros 2 integrantes como colaboradores:
**GitHub → Repositorio → Settings → Collaborators → Add people**

Cada integrante:

```bash
# 1. Clonar
git clone https://github.com/victor99a/Cloud-Native.git
cd Cloud-Native

# 2. Configurar identidad (una sola vez)
git config user.name "Tu Nombre"
git config user.email "tu@correo.com"

# 3. Crear su rama desde main y cambiar a ella
git checkout -b feature/backend-bff        # (cambiar por su rama)

# 4. Trabajar y commitear
git add .
git commit -m "feat(backend): implementar PedidoController"

# 5. Subir la rama
git push -u origin feature/backend-bff
```

Luego abrir un **Pull Request** en GitHub (rama → `main`) y mergearlo.

### Opción B — Fork + Pull Request (si no hay acceso de colaborador)

```bash
# 1. Hacer fork desde GitHub (botón "Fork")
# 2. Clonar el fork
git clone https://github.com/TU_USUARIO/Cloud-Native.git
cd Cloud-Native

# 3. Crear rama y trabajar
git checkout -b feature/backend-bff
git add .
git commit -m "feat(backend): ..."
git push -u origin feature/backend-bff

# 4. Abrir Pull Request desde tu fork hacia victor99a/Cloud-Native
```

## Convención de commits

```
feat(area): descripción     # nueva funcionalidad
fix(area): descripción      # corrección de bug
docs(area): descripción     # documentación
refactor(area): descripción # refactor sin cambio de comportamiento
style(area): descripción    # formato/estilo
```

Ejemplos: `feat(backend)`, `feat(core)`, `feat(ui)`.

## Reglas

1. **No editar** carpetas que no te corresponden (evita conflictos).
2. Hacer **commits pequeños y frecuentes**.
3. Antes de mergear, hacer `git pull origin main` en tu rama para estar al día.
4. Cada integrante debe tener **al menos una rama con commits propios** visible en GitHub.
