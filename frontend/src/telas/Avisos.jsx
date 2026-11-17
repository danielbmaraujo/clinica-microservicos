import { useEffect, useState } from 'react';
import { listarNotificacoes, listarPacientes } from '../api.js';
import Mensagem from '../Mensagem.jsx';

// Tela 3: escolher um paciente e ver as notificações dele.
export default function Avisos() {
  const [pacientes, setPacientes] = useState([]);
  const [pacienteId, setPacienteId] = useState('');
  const [avisos, setAvisos] = useState([]);
  const [erro, setErro] = useState('');

  useEffect(() => {
    listarPacientes().then(setPacientes).catch((e) => setErro(e.message));
  }, []);

  async function carregarAvisos() {
    setErro('');
    if (!pacienteId) {
      setAvisos([]);
      return;
    }
    try {
      setAvisos(await listarNotificacoes(pacienteId));
    } catch (e) {
      setErro(e.message);
    }
  }
  useEffect(() => {
    carregarAvisos();
  }, [pacienteId]);

  return (
    <section>
      <h2>Avisos do paciente</h2>
      <div className="linha">
        <select value={pacienteId} onChange={(e) => setPacienteId(e.target.value)}>
          <option value="">Selecione um paciente...</option>
          {pacientes.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nome}
            </option>
          ))}
        </select>
        <button onClick={carregarAvisos} disabled={!pacienteId}>
          Atualizar
        </button>
      </div>

      <Mensagem erro={erro} />

      {pacienteId && avisos.length === 0 && !erro && <p>Nenhum aviso para este paciente.</p>}
      <ul className="avisos">
        {avisos.map((a) => (
          <li key={a.id}>
            <strong>{a.tipo}</strong> · {new Date(a.dataEnvio).toLocaleString('pt-BR')}
            <br />
            {a.mensagem}
          </li>
        ))}
      </ul>
    </section>
  );
}
