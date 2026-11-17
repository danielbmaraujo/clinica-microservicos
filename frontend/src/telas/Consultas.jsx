import { useEffect, useState } from 'react';
import {
  cancelarConsulta,
  listarConsultas,
  listarHorariosLivres,
  listarPacientes,
  listarProfissionais,
  marcarConsulta,
  remarcarConsulta,
} from '../api.js';
import { formatarDataHora, hoje } from '../formatar.js';
import Mensagem from '../Mensagem.jsx';

// Tela 2: marcar consulta (paciente -> profissional -> data -> horário livre)
// e lista de consultas com botões remarcar e cancelar.
export default function Consultas() {
  const [pacientes, setPacientes] = useState([]);
  const [profissionais, setProfissionais] = useState([]);
  const [consultas, setConsultas] = useState([]);

  // Escolhas do formulário de marcação
  const [pacienteId, setPacienteId] = useState('');
  const [profissionalId, setProfissionalId] = useState('');
  const [data, setData] = useState('');
  const [horarios, setHorarios] = useState([]);
  const [horario, setHorario] = useState('');

  // Remarcação: qual consulta está sendo remarcada e a nova data/horário
  const [remarcando, setRemarcando] = useState(null);
  const [novaData, setNovaData] = useState('');
  const [novosHorarios, setNovosHorarios] = useState([]);
  const [novoHorario, setNovoHorario] = useState('');

  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');

  function limparMensagens() {
    setErro('');
    setSucesso('');
  }

  // Ao abrir a tela: carrega pacientes e profissionais (cada um vem de um serviço diferente)
  useEffect(() => {
    listarPacientes().then(setPacientes).catch((e) => setErro(e.message));
    listarProfissionais().then(setProfissionais).catch((e) => setErro(e.message));
  }, []);

  // Lista de consultas: do paciente escolhido, ou todas
  async function carregarConsultas() {
    try {
      setConsultas(await listarConsultas(pacienteId));
    } catch (e) {
      setErro(e.message);
    }
  }
  useEffect(() => {
    carregarConsultas();
  }, [pacienteId]);

  // Horários livres: recarrega quando muda o profissional ou a data
  async function carregarHorarios() {
    setHorario('');
    if (!profissionalId || !data) {
      setHorarios([]);
      return;
    }
    try {
      setHorarios(await listarHorariosLivres(profissionalId, data));
    } catch (e) {
      setErro(e.message);
    }
  }
  useEffect(() => {
    carregarHorarios();
  }, [profissionalId, data]);

  // Horários livres para a remarcação (do mesmo profissional da consulta)
  useEffect(() => {
    setNovoHorario('');
    if (!remarcando || !novaData) {
      setNovosHorarios([]);
      return;
    }
    listarHorariosLivres(remarcando.profissionalId, novaData)
      .then(setNovosHorarios)
      .catch((e) => setErro(e.message));
  }, [remarcando, novaData]);

  async function marcar(e) {
    e.preventDefault();
    limparMensagens();
    try {
      const consulta = await marcarConsulta(Number(pacienteId), Number(profissionalId), `${data}T${horario}`);
      setSucesso(`Consulta marcada para ${formatarDataHora(consulta.dataHora)}.`);
      carregarConsultas();
      carregarHorarios();
    } catch (e) {
      setErro(e.message); // ex.: "Paciente inativo", "Serviço de Pacientes indisponível"
    }
  }

  function abrirRemarcacao(consulta) {
    limparMensagens();
    setRemarcando(consulta);
    setNovaData('');
  }

  async function confirmarRemarcacao() {
    limparMensagens();
    try {
      const consulta = await remarcarConsulta(remarcando.id, `${novaData}T${novoHorario}`);
      setSucesso(`Consulta remarcada para ${formatarDataHora(consulta.dataHora)}.`);
      setRemarcando(null);
      carregarConsultas();
      carregarHorarios();
    } catch (e) {
      setErro(e.message);
    }
  }

  async function cancelar(consulta) {
    limparMensagens();
    try {
      await cancelarConsulta(consulta.id);
      setSucesso(`Consulta de ${formatarDataHora(consulta.dataHora)} cancelada.`);
      carregarConsultas();
      carregarHorarios();
    } catch (e) {
      setErro(e.message);
    }
  }

  // Mostra o nome em vez do id (os dados vêm de serviços diferentes)
  function nomePaciente(id) {
    return pacientes.find((p) => p.id === id)?.nome || `Paciente ${id}`;
  }
  function nomeProfissional(id) {
    const prof = profissionais.find((p) => p.id === id);
    return prof ? `${prof.nome} (${prof.especialidade})` : `Profissional ${id}`;
  }

  return (
    <section>
      <h2>Marcar consulta</h2>
      <form className="formulario" onSubmit={marcar}>
        <label>
          1. Paciente
          <select value={pacienteId} onChange={(e) => setPacienteId(e.target.value)} required>
            <option value="">Selecione...</option>
            {pacientes
              .filter((p) => p.ativo)
              .map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nome}
                </option>
              ))}
          </select>
        </label>
        <label>
          2. Profissional
          <select value={profissionalId} onChange={(e) => setProfissionalId(e.target.value)} required>
            <option value="">Selecione...</option>
            {profissionais.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nome} — {p.especialidade}
              </option>
            ))}
          </select>
        </label>
        <label>
          3. Data
          <input type="date" value={data} min={hoje()} onChange={(e) => setData(e.target.value)} required />
        </label>

        {profissionalId && data && (
          <div className="horarios">
            4. Horário livre:
            {horarios.length === 0 && <span> nenhum horário livre neste dia (fim de semana ou agenda cheia).</span>}
            {horarios.map((h) => (
              <button
                type="button"
                key={h}
                className={h === horario ? 'ativo' : ''}
                onClick={() => setHorario(h)}
              >
                {h}
              </button>
            ))}
          </div>
        )}

        <button type="submit" disabled={!pacienteId || !profissionalId || !data || !horario}>
          Marcar consulta
        </button>
      </form>

      <Mensagem erro={erro} sucesso={sucesso} />

      <h2>{pacienteId ? `Consultas de ${nomePaciente(Number(pacienteId))}` : 'Todas as consultas'}</h2>
      <table>
        <thead>
          <tr>
            <th>Id</th>
            <th>Paciente</th>
            <th>Profissional</th>
            <th>Data e hora</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {consultas.map((c) => (
            <tr key={c.id} className={c.status === 'CANCELADA' ? 'inativo' : ''}>
              <td>{c.id}</td>
              <td>{nomePaciente(c.pacienteId)}</td>
              <td>{nomeProfissional(c.profissionalId)}</td>
              <td>{formatarDataHora(c.dataHora)}</td>
              <td>{c.status}</td>
              <td>
                {c.status !== 'CANCELADA' && (
                  <>
                    <button onClick={() => abrirRemarcacao(c)}>Remarcar</button>{' '}
                    <button onClick={() => cancelar(c)}>Cancelar</button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {consultas.length === 0 && <p>Nenhuma consulta.</p>}

      {remarcando && (
        <div className="caixa">
          <h3>
            Remarcar consulta {remarcando.id} ({formatarDataHora(remarcando.dataHora)})
          </h3>
          <label>
            Nova data{' '}
            <input type="date" value={novaData} min={hoje()} onChange={(e) => setNovaData(e.target.value)} />
          </label>
          {novaData && (
            <div className="horarios">
              Horário livre:
              {novosHorarios.length === 0 && <span> nenhum horário livre neste dia.</span>}
              {novosHorarios.map((h) => (
                <button
                  type="button"
                  key={h}
                  className={h === novoHorario ? 'ativo' : ''}
                  onClick={() => setNovoHorario(h)}
                >
                  {h}
                </button>
              ))}
            </div>
          )}
          <div className="linha">
            <button onClick={confirmarRemarcacao} disabled={!novaData || !novoHorario}>
              Confirmar remarcação
            </button>
            <button onClick={() => setRemarcando(null)}>Fechar</button>
          </div>
        </div>
      )}
    </section>
  );
}
