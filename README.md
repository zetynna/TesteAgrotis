# Agrotis Full-Stack Challenge

Aplicação de cadastros agro, consolidação analítico-financeira e integração resiliente, baseada no PDF do desafio.

## Arquitetura

```text
React + TypeScript (5173)
       │ REST/JSON
       ▼
Spring Boot 3 (8080) ── fila transacional em tabela sync_batches
       │                         │ worker agendado, retry e DLQ
       ├── produtores             └── payload persistido antes do processamento
       ├── propriedades
       ├── laboratórios
       └── analytics
       │
       ▼
H2 + Flyway (arquivo local; banco em memória isolado nos testes)
```

O backend usa Java 17+, Spring Boot, Spring Data JPA, Bean Validation e Flyway. O frontend usa React, TypeScript e Vite. A fila relacional é a outbox durável: a requisição é gravada e confirmada antes de o worker agendado processá-la. Não há necessidade de broker externo.

## Execução local

Pré-requisitos: JDK 17+, Maven 3.9+ e Node.js 22+.

```bash
cd Backend/agrotis-backend
mkdir -p data
mvn spring-boot:run
```

Em outro terminal:

```bash
cd Frontend
npm ci
npm run dev
```

Aplicação: <http://localhost:5173>. API: <http://localhost:8080>; Swagger UI: <http://localhost:8080/swagger-ui.html>; health: <http://localhost:8080/actuator/health>. H2 Console: <http://localhost:8080/h2-console>, JDBC URL `jdbc:h2:file:./data/agrotis`, usuário `sa`, senha vazia. A URL do banco pode ser substituída por `AGROTIS_DB_URL`. O arquivo local do banco é ignorado pelo Git.

Consulta de CEP usa ViaCEP, sem token ou credencial; a base URL pode ser configurada em `VIACEP_API_BASE_URL`.

## Docker

Na raiz do repositório, com Docker Compose instalado:

```bash
docker compose up --build
```

Frontend: <http://localhost:5173>; API: <http://localhost:8080>. O Compose mantém o H2 em volume nomeado `agrotis-db`. Para encerrar, use `docker compose down`; para também remover os dados persistidos do banco local, `docker compose down -v`.

## Testes e validação

```bash
cd Backend/agrotis-backend && mvn test
cd Frontend && npm run build && npm run lint
```

Os testes do backend usam H2 em memória via `src/test/resources/application.yml`; não dependem de dados locais nem de credenciais.

## Cadastros e relacionamento

- Produtores, propriedades e laboratórios possuem nome, CPF/CNPJ validado e endereço.
- Um produtor pode possuir várias propriedades; cada propriedade tem no máximo um produtor responsável. Laboratórios mantêm vínculo N:1 com produtores.
- `V2__integrations_and_property_owner.sql` cria a FK de propriedade para produtor sem apagar a tabela legada `producer_properties`. Apenas vínculos legados unívocos são migrados automaticamente; associações ambíguas ficam preservadas na tabela antiga, sem uma escolha automática de proprietário. Reconcilie esses casos manualmente antes de remover a tabela arquivada.
- A edição de produtor rejeita propriedade que já pertença a outro produtor. O cadastro de propriedades impede excluir uma propriedade vinculada; laboratórios também não são excluídos enquanto houver produtores associados.
- Propriedades e laboratórios têm CRUD em `/api/properties` e `/api/laboratories`, com JSON validado no corpo da requisição.

## Contrato de integração v1

### Ingestão e idempotência

`POST /api/integration/v1/sync` aceita um registro por chamada. O corpo usa `schemaVersion: 1`, `source`, `externalReference`, `entityType` (`PRODUCER`, `PROPERTY` ou `LABORATORY`) e `payload` JSON. Exemplo de produtor:

```json
{
  "schemaVersion": 1,
  "source": "ERP",
  "externalReference": "producer-2026-001",
  "entityType": "PRODUCER",
  "payload": {
    "nome": "Manoel da Silva Ribeiro",
    "inscricaoFiscal": "123.456.789-09",
    "endereco": {
      "logradouro": "Rua Principal, 100",
      "cidade": "Pouso Alegre",
      "estado": "MG",
      "cep": "37500-000"
    },
    "dataInicial": "2026-06-11T12:00:00Z",
    "dataFinal": "2026-06-15T18:00:00Z",
    "observacoes": "Iniciando testes de integração",
    "laboratorio": {
      "nome": "Laboratório Modelo 1",
      "inscricaoFiscal": "11.222.333/0001-81",
      "endereco": {
        "logradouro": "Av. Central, 100",
        "cidade": "Belo Horizonte",
        "estado": "MG",
        "cep": "30100-000"
      }
    },
    "propriedades": [
      {
        "nome": "Fazenda Sol Nascente",
        "inscricaoFiscal": "987.654.321-00",
        "endereco": {
          "logradouro": "Rodovia BR 101, Km 45",
          "cidade": "Itajubá",
          "estado": "MG",
          "cep": "37500-000"
        }
      }
    ]
  }
}
```

`payload` de propriedade segue `{nome, inscricaoFiscal, endereco, produtorInscricaoFiscal?}`; o CPF/CNPJ do produtor responsável é opcional, mas, quando enviado, precisa corresponder a produtor já cadastrado. `payload` de laboratório segue `{nome, inscricaoFiscal, endereco}`. Endereço contém `logradouro`, `cidade`, `estado` e `cep`; datas de produtor aceitam ISO-8601 com offset. As inscrições fiscais devem passar validação de dígitos.

O serviço salva lote, payload e hash SHA-256 do JSON canonizado antes de responder `202 Accepted`. A chave idempotente é `(source, externalReference)`: mesma chave, tipo e conteúdo retorna o mesmo lote; reutilizar a chave com tipo/conteúdo diferente resulta em `409 Conflict`. Produtor, propriedade e laboratório são reconciliados por inscrição fiscal. Propriedade já vinculada a outro produtor não é reassociada implicitamente: o lote é encaminhado à DLQ com erro observável.

O payload completo não é retornado em endpoints de monitoramento e não é escrito nos logs. As respostas incluem tipo/versão, status, tentativas, erro sanitizado e timestamps.

### Retentativas, DLQ e monitoramento

Transições usuais: `PENDING → IN_PROGRESS → SUCCEEDED`; falha transitória gera `FAILED` e agenda nova tentativa; falha de dados/validação vai diretamente a `DLQ`. Configuração padrão: máximo de 5 tentativas, espera inicial de 2 segundos e teto de 300 segundos, com atraso exponencial `min(teto, esperaInicial × 2^(tentativa−1))`. A contagem é por ciclo; reprocessamento manual reinicia o ciclo. Lotes `IN_PROGRESS` têm lease de 900 segundos e são recuperados após expirar, por exemplo, depois de interrupção do processo.

- `GET /api/integration/batches`: todos os lotes, mais recentes primeiro.
- `GET /api/integration/dlq`: lotes na DLQ.
- `POST /api/integration/batches/{id}/reprocess`: refile de lote `DLQ` ou `FAILED`; outros estados retornam conflito.

A tela **Monitoramento de integração** acompanha status/tentativas, mostra erros e permite reprocessar manualmente. Os logs estruturados registram origem, referência, tipo, ID do lote, tentativa e transição, nunca o conteúdo do payload.

## Analítica e financeiro

`GET /api/analytics/laboratories` requer `qtdMinimaProdutores` inteiro não negativo. `dataInicialInicio`/`dataInicialFim` e `dataFinalInicio`/`dataFinalFim` são inclusivos; início posterior ao fim retorna 400. `buscaTexto` usa correspondência parcial, sem distinção entre maiúsculas/minúsculas, em `observacoes`. Os filtros de produtor são aplicados antes da contagem e do cálculo; só laboratórios com pelo menos a contagem mínima são retornados. A ordenação é quantidade de produtores descendente; quando algum filtro de data inicial estiver presente, desempata pela menor `dataInicial` ascendente e depois pelo ID; sem filtro de data inicial, o desempate é pelo ID ascendente. O nome da resposta está em maiúsculas.

O PDF especifica a natureza das parcelas financeiras, mas não publica valores, bases ou taxas. Por isso, não há valores monetários inventados: todas as taxas são `BigDecimal` e têm padrão zero. Fórmula implementada:

```text
comissão do produtor = dias completos não negativos entre dataInicial e dataFinal × taxa de comissão/dia
operação territorial da propriedade = 1 se a propriedade tiver produtor responsável, senão 0 × taxa/propriedade
taxa territorial do laboratório = quantidade de produtores filtrados × taxa/produtor
consolidado = soma das comissões dos produtores + taxas de suas propriedades distintas + taxa territorial do laboratório
```

Produtor sem uma das datas recebe comissão zero. Taxas são valores monetários em reais por unidade (não percentuais):

- `AGROTIS_FINANCE_PRODUCER_COMMISSION_RATE` — comissão por dia.
- `AGROTIS_FINANCE_PROPERTY_OPERATIONAL_RATE` — taxa operacional por propriedade vinculada.
- `AGROTIS_FINANCE_LABORATORY_TERRITORIAL_RATE` — taxa territorial por produtor contado.

Os três padrões são `0`. Defina os valores somente após confirmação das bases/taxas de negócio. A taxa operacional de propriedade é consolidada por propriedade distinta dentro dos produtores filtrados.

## APIs principais

- Produtores: `GET/POST /api/producers`, `PUT/DELETE /api/producers/{id}`.
- Propriedades e laboratórios: `GET/POST /api/properties`, `PUT/DELETE /api/properties/{id}`; os mesmos métodos para `/api/laboratories`.
- Analítica: `GET /api/analytics/laboratories?qtdMinimaProdutores=1`.
- Integração: `POST /api/integration/v1/sync`, `GET /api/integration/batches`, `GET /api/integration/dlq`, `POST /api/integration/batches/{id}/reprocess`.
- CEP: `GET /api/cep/{cep}`.
- OpenAPI: `/v3/api-docs` e `/swagger-ui.html`.
