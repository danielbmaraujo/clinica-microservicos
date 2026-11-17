// Funções de formatação de datas para mostrar na tela.

// "2026-11-20T09:00" -> "20/11/2026 às 09:00"
export function formatarDataHora(dataHora) {
  const [data, hora] = dataHora.split('T');
  return `${formatarData(data)} às ${hora}`;
}

// "1995-04-10" -> "10/04/1995"
export function formatarData(data) {
  if (!data) return '—';
  const [ano, mes, dia] = data.split('-');
  return `${dia}/${mes}/${ano}`;
}

// Data de hoje no formato "YYYY-MM-DD" (usada como mínimo no campo de data)
export function hoje() {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}
