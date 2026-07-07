// Teste de carga do Extrato de Liquidação (k6) — critério P5 de docs/criterios-aceite.md.
// Mede as 3 consultas do requisito (período, cedente e moeda) sob volume real,
// cada uma com métrica própria via tag `consulta`.
//
// Rode via ./carga-extrato.sh (usa o container grafana/k6, nada pra instalar).
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const DIA_MS = 86_400_000;

export const options = {
  scenarios: {
    extrato: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 8),
      duration: __ENV.DURACAO || '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{consulta:cedente_periodo}': ['p(95)<300'],
    'http_req_duration{consulta:moeda_periodo}': ['p(95)<300'],
    'http_req_duration{consulta:sem_filtro}': ['p(95)<800'],
  },
};

export function setup() {
  const res = http.get(`${BASE}/api/cedentes`);
  const cedentes = res
    .json()
    .filter((c) => c.documento && c.documento.startsWith('CARGA-'))
    .map((c) => c.id);
  if (cedentes.length === 0) {
    throw new Error('Nenhum cedente CARGA-* encontrado — rode ./seed.sh antes.');
  }
  return { cedentes };
}

// Janela aleatória de 30 dias dentro dos 2 anos semeados.
function janela() {
  const fim = Date.now() - Math.random() * 700 * DIA_MS;
  return {
    dataInicio: new Date(fim - 30 * DIA_MS).toISOString(),
    dataFim: new Date(fim).toISOString(),
  };
}

function consulta(nome, params) {
  const qs = Object.entries(params)
    .map(([k, v]) => `${k}=${encodeURIComponent(v)}`)
    .join('&');
  const res = http.get(`${BASE}/api/relatorios/extrato-liquidacao?${qs}`, {
    tags: { consulta: nome },
  });
  check(res, { 'status 200': (r) => r.status === 200 });
}

export default function (data) {
  const { dataInicio, dataFim } = janela();
  const cedenteId = data.cedentes[Math.floor(Math.random() * data.cedentes.length)];
  const pagina = Math.floor(Math.random() * 50);

  // A consulta mais comum do requisito: cedente + período (covering index).
  consulta('cedente_periodo', { cedenteId, dataInicio, dataFim, page: pagina, size: 20 });

  // Filtro por moeda + período.
  consulta('moeda_periodo', {
    moeda: Math.random() < 0.5 ? 'BRL' : 'USD',
    dataInicio,
    dataFim,
    page: pagina,
    size: 20,
  });

  // Pior caso: sem filtro nenhum, paginação profunda (count sobre a tabela inteira).
  consulta('sem_filtro', { page: Math.floor(Math.random() * 500), size: 20 });
}
