# Deploy no Railway

Cada pasta do repositório vira **um serviço** no Railway. Cada serviço tem um `railway.json` na própria pasta com o comando de build/start (o Railway lê esse arquivo sozinho).

## 1. Criar os serviços

No projeto do Railway:

1. **Bancos** (+ New → Database). Depois de criar, **renomeie** cada um (Settings → nome do serviço) exatamente assim, porque as referências abaixo usam esses nomes:
   - MySQL → `MySQL-Pacientes`
   - MySQL → `MySQL-Agendamento`
   - MongoDB → `MongoDB`
2. **Serviços do código** (+ New → GitHub Repo → este repositório), 4 vezes. Em cada um, em **Settings → Root Directory**, coloque a pasta e dê o nome:

| Nome do serviço no Railway | Root Directory |
|---|---|
| `servico-pacientes` | `servico-pacientes` |
| `servico-agendamento` | `servico-agendamento` |
| `servico-notificacoes` | `servico-notificacoes` |
| `frontend` | `frontend` |

3. Em cada um dos 4 serviços: **Settings → Networking → Generate Domain** (gera a URL pública `https://xxx.up.railway.app`). Se ele perguntar a porta, aceite a sugerida (é a da variável `PORT`, que o Railway define sozinho).

> Os nomes precisam bater: `${{servico-pacientes.RAILWAY_PUBLIC_DOMAIN}}` só funciona se o serviço se chamar `servico-pacientes`. Se você usar outros nomes, troque nas tabelas abaixo.

## 2. Variáveis de cada serviço (aba **Variables**)

Copie os valores exatamente como estão (com `${{ }}`); o Railway substitui pelo valor real.
**Não** crie a variável `PORT`: o Railway já define sozinho.

### servico-pacientes (Python)

| Variável | Valor |
|---|---|
| `MYSQLHOST` | `${{MySQL-Pacientes.MYSQLHOST}}` |
| `MYSQLPORT` | `${{MySQL-Pacientes.MYSQLPORT}}` |
| `MYSQLUSER` | `${{MySQL-Pacientes.MYSQLUSER}}` |
| `MYSQLPASSWORD` | `${{MySQL-Pacientes.MYSQLPASSWORD}}` |
| `MYSQLDATABASE` | `${{MySQL-Pacientes.MYSQLDATABASE}}` |
| `FRONTEND_URL` | `https://${{frontend.RAILWAY_PUBLIC_DOMAIN}}` |

### servico-agendamento (Java)

| Variável | Valor |
|---|---|
| `MYSQLHOST` | `${{MySQL-Agendamento.MYSQLHOST}}` |
| `MYSQLPORT` | `${{MySQL-Agendamento.MYSQLPORT}}` |
| `MYSQLUSER` | `${{MySQL-Agendamento.MYSQLUSER}}` |
| `MYSQLPASSWORD` | `${{MySQL-Agendamento.MYSQLPASSWORD}}` |
| `MYSQLDATABASE` | `${{MySQL-Agendamento.MYSQLDATABASE}}` |
| `PACIENTES_URL` | `https://${{servico-pacientes.RAILWAY_PUBLIC_DOMAIN}}` |
| `NOTIFICACOES_URL` | `https://${{servico-notificacoes.RAILWAY_PUBLIC_DOMAIN}}` |
| `FRONTEND_URL` | `https://${{frontend.RAILWAY_PUBLIC_DOMAIN}}` |
| `JAVA_TOOL_OPTIONS` | `-Xmx300m` |

### servico-notificacoes (Node)

| Variável | Valor |
|---|---|
| `MONGO_URL` | `${{MongoDB.MONGO_URL}}` |
| `FRONTEND_URL` | `https://${{frontend.RAILWAY_PUBLIC_DOMAIN}}` |

### frontend (React)

| Variável | Valor |
|---|---|
| `VITE_PACIENTES_URL` | `https://${{servico-pacientes.RAILWAY_PUBLIC_DOMAIN}}` |
| `VITE_AGENDAMENTO_URL` | `https://${{servico-agendamento.RAILWAY_PUBLIC_DOMAIN}}` |
| `VITE_NOTIFICACOES_URL` | `https://${{servico-notificacoes.RAILWAY_PUBLIC_DOMAIN}}` |

> As variáveis `VITE_*` são lidas **na hora do build** (ficam "gravadas" no JavaScript). Se mudar alguma, faça **Redeploy** do frontend.

## 3. Ordem sugerida

1. Bancos → 2. `servico-pacientes` e `servico-notificacoes` → 3. `servico-agendamento` → 4. `frontend`.
Depois de configurar as variáveis, clique em **Deploy** em cada serviço.

## 4. Conferir

Abra no navegador (troque pelo domínio de cada um):

- `https://<pacientes>.up.railway.app/health` → `{"status":"ok","servico":"pacientes"}`
- `https://<agendamento>.up.railway.app/health` → `{"status":"ok","servico":"agendamento"}`
- `https://<notificacoes>.up.railway.app/health` → `{"status":"ok","servico":"notificacoes"}`
- `https://<frontend>.up.railway.app` → a tela do sistema

As tabelas são criadas sozinhas na primeira vez e os dados de exemplo (3 pacientes, profissionais) também.

## 5. Como cada serviço sobe (já configurado nos `railway.json`)

| Serviço | Build | Start |
|---|---|---|
| Pacientes | instala `requirements.txt` | `uvicorn main:app --host 0.0.0.0 --port $PORT` |
| Agendamento | `mvn -B -DskipTests clean package` → gera `target/servico-agendamento.jar` | `java -jar target/servico-agendamento.jar` |
| Notificações | `npm install` | `npm start` |
| Frontend | `npm run build` → gera `dist/` | `npm start` (= `vite preview` na porta `$PORT`) |

## 6. Problemas comuns

- **Tela mostra "Serviço indisponível"** → confira as `VITE_*` do frontend (com `https://`) e faça Redeploy do frontend.
- **Erro de CORS no console do navegador** → confira o `FRONTEND_URL` nos 3 serviços (com `https://`, sem `/` no final — se tiver, o código remove).
- **Agendamento reinicia sozinho / "Out of memory"** → confira `JAVA_TOOL_OPTIONS=-Xmx300m`.
- **Variável aparece vazia** → o nome do serviço no Railway não bate com o da referência `${{Nome.VARIAVEL}}`.
