import { useState } from 'react';
import Pacientes from './telas/Pacientes.jsx';
import Consultas from './telas/Consultas.jsx';
import Avisos from './telas/Avisos.jsx';

// Menu simples no topo: guarda qual tela está aberta.
export default function App() {
  const [tela, setTela] = useState('pacientes');

  return (
    <>
      <header>
        <h1>Clínica</h1>
        <nav>
          <button className={tela === 'pacientes' ? 'ativo' : ''} onClick={() => setTela('pacientes')}>
            Pacientes
          </button>
          <button className={tela === 'consultas' ? 'ativo' : ''} onClick={() => setTela('consultas')}>
            Consultas
          </button>
          <button className={tela === 'avisos' ? 'ativo' : ''} onClick={() => setTela('avisos')}>
            Avisos
          </button>
        </nav>
      </header>

      <main>
        {tela === 'pacientes' && <Pacientes />}
        {tela === 'consultas' && <Consultas />}
        {tela === 'avisos' && <Avisos />}
      </main>
    </>
  );
}
