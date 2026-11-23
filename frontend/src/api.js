// Todas as chamadas aos 3 microsserviços ficam aqui.
// O frontend conversa com cada serviço diretamente, pela rede (HTTP + JSON).

// As URLs vêm das variáveis VITE_* (lidas na hora do build); sem elas, usa os serviços locais.
export const PACIENTES_URL = import.meta.env.VITE_PACIENTES_URL || 'http://localhost:8001';
export const AGENDAMENTO_URL = import.meta.env.VITE_AGENDAMENTO_URL || 'http://localhost:8002';
export const NOTIFICACOES_URL = import.meta.env.VITE_NOTIFICACOES_URL || 'http://localhost:8003';

// Faz a requisição e devolve o JSON da resposta.
// Se a API responder erro, lança um Error com a mensagem do campo "erro".
async function requisicao(url, metodo = 'GET', corpo) {
  let resposta;
  try {
    resposta = await fetch(url, {
      method: metodo,
      headers: corpo ? { 'Content-Type': 'application/json' } : {},
      body: corpo ? JSON.stringify(corpo) : undefined,
    });
  } catch {
    // fetch só falha assim quando o serviço está fora do ar
    throw new Error(`Serviço indisponível: ${new URL(url).origin}`);
  }

  if (resposta.status === 204) return null; // sem conteúdo (ex.: DELETE)

  const dados = await resposta.json();
  if (!resposta.ok) {
    throw new Error(dados.erro || `Erro ${resposta.status}`);
  }
  return dados;
}

// ---------- Serviço de Pacientes ----------

export function listarPacientes(busca = '') {
  const filtro = busca ? `?busca=${encodeURIComponent(busca)}` : '';
  return requisicao(`${PACIENTES_URL}/pacientes${filtro}`);
}

export function cadastrarPaciente(paciente) {
  return requisicao(`${PACIENTES_URL}/pacientes`, 'POST', paciente);
}

export function desativarPaciente(id) {
  return requisicao(`${PACIENTES_URL}/pacientes/${id}`, 'DELETE');
}

// ---------- Serviço de Agendamento ----------

export function listarProfissionais() {
  return requisicao(`${AGENDAMENTO_URL}/profissionais`);
}

export function listarHorariosLivres(profissionalId, data) {
  return requisicao(`${AGENDAMENTO_URL}/horarios-livres?profissionalId=${profissionalId}&data=${data}`);
}

export function listarConsultas(pacienteId) {
  const filtro = pacienteId ? `?pacienteId=${pacienteId}` : '';
  return requisicao(`${AGENDAMENTO_URL}/consultas${filtro}`);
}

export function marcarConsulta(pacienteId, profissionalId, dataHora) {
  return requisicao(`${AGENDAMENTO_URL}/consultas`, 'POST', { pacienteId, profissionalId, dataHora });
}

export function remarcarConsulta(id, dataHora) {
  return requisicao(`${AGENDAMENTO_URL}/consultas/${id}/remarcar`, 'PUT', { dataHora });
}

export function cancelarConsulta(id) {
  return requisicao(`${AGENDAMENTO_URL}/consultas/${id}/cancelar`, 'PUT');
}

// ---------- Serviço de Notificações ----------

export function listarNotificacoes(pacienteId) {
  return requisicao(`${NOTIFICACOES_URL}/notificacoes/paciente/${pacienteId}`);
}
