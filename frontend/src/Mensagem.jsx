// Caixa de mensagem: vermelha para erro, verde para sucesso.
export default function Mensagem({ erro, sucesso }) {
  if (erro) return <p className="mensagem erro">{erro}</p>;
  if (sucesso) return <p className="mensagem sucesso">{sucesso}</p>;
  return null;
}
