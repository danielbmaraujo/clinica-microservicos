import { useEffect, useState } from 'react';
import { cadastrarPaciente, desativarPaciente, listarPacientes } from '../api.js';
import { formatarData } from '../formatar.js';
import Mensagem from '../Mensagem.jsx';

const FORM_VAZIO = { nome: '', cpf: '', dataNascimento: '', telefone: '', email: '' };

// Tela 1: cadastro de pacientes + lista com busca + botão desativar
export default function Pacientes() {
  const [pacientes, setPacientes] = useState([]);
  const [busca, setBusca] = useState('');
  const [form, setForm] = useState(FORM_VAZIO);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');

  async function carregar(texto) {
    try {
      setPacientes(await listarPacientes(texto));
    } catch (e) {
      setErro(e.message);
    }
  }

  // Carrega a lista ao abrir a tela
  useEffect(() => {
    carregar('');
  }, []);

  function alterarCampo(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function cadastrar(e) {
    e.preventDefault();
    setErro('');
    setSucesso('');
    const dados = {
      nome: form.nome,
      cpf: form.cpf.replace(/\D/g, ''), // aceita "529.982.247-25" e manda só os números
      dataNascimento: form.dataNascimento || null,
      telefone: form.telefone || null,
      email: form.email || null,
    };
    try {
      const novo = await cadastrarPaciente(dados);
      setSucesso(`Paciente ${novo.nome} cadastrado (id ${novo.id}).`);
      setForm(FORM_VAZIO);
      carregar(busca);
    } catch (e) {
      setErro(e.message); // ex.: "CPF já cadastrado"
    }
  }

  async function desativar(paciente) {
    setErro('');
    setSucesso('');
    try {
      await desativarPaciente(paciente.id);
      setSucesso(`Paciente ${paciente.nome} desativado.`);
      carregar(busca);
    } catch (e) {
      setErro(e.message);
    }
  }

  function buscar(e) {
    e.preventDefault();
    carregar(busca);
  }

  return (
    <section>
      <h2>Cadastrar paciente</h2>
      <form className="formulario" onSubmit={cadastrar}>
        <label>
          Nome*
          <input name="nome" value={form.nome} onChange={alterarCampo} required />
        </label>
        <label>
          CPF*
          <input name="cpf" value={form.cpf} onChange={alterarCampo} placeholder="somente números" required />
        </label>
        <label>
          Data de nascimento
          <input type="date" name="dataNascimento" value={form.dataNascimento} onChange={alterarCampo} />
        </label>
        <label>
          Telefone
          <input name="telefone" value={form.telefone} onChange={alterarCampo} />
        </label>
        <label>
          E-mail
          <input type="email" name="email" value={form.email} onChange={alterarCampo} />
        </label>
        <button type="submit">Cadastrar</button>
      </form>

      <Mensagem erro={erro} sucesso={sucesso} />

      <h2>Pacientes</h2>
      <form className="linha" onSubmit={buscar}>
        <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar por nome ou CPF" />
        <button type="submit">Buscar</button>
      </form>

      <table>
        <thead>
          <tr>
            <th>Id</th>
            <th>Nome</th>
            <th>CPF</th>
            <th>Nascimento</th>
            <th>Telefone</th>
            <th>E-mail</th>
            <th>Situação</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {pacientes.map((p) => (
            <tr key={p.id} className={p.ativo ? '' : 'inativo'}>
              <td>{p.id}</td>
              <td>{p.nome}</td>
              <td>{p.cpf}</td>
              <td>{formatarData(p.dataNascimento)}</td>
              <td>{p.telefone || '—'}</td>
              <td>{p.email || '—'}</td>
              <td>{p.ativo ? 'Ativo' : 'Inativo'}</td>
              <td>{p.ativo && <button onClick={() => desativar(p)}>Desativar</button>}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {pacientes.length === 0 && <p>Nenhum paciente encontrado.</p>}
    </section>
  );
}
