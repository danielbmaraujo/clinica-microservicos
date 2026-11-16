// Serviço de Notificações — Express na porta 8003.
// Rodar: npm start
const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const { Notificacao, TIPOS } = require('./Notificacao');

// Configurações vindas de variáveis de ambiente (com valor padrão para rodar local)
const PORTA = process.env.PORT || 8003;
const MONGO_URL =
  process.env.MONGO_URL ||
  'mongodb://clinica:clinica123@localhost:27017/db_notificacoes?authSource=admin';

const app = express();
app.use(cors({ origin: 'http://localhost:3000' })); // libera o frontend React
app.use(express.json()); // lê o corpo das requisições como JSON

// ---------- Rotas ----------

app.get('/health', (req, res) => {
  res.json({ status: 'ok', servico: 'notificacoes' });
});

// Registra (e "envia") uma notificação
app.post('/notificacoes', async (req, res) => {
  const { pacienteId, consultaId, tipo, mensagem } = req.body || {};

  // Validações -> 400
  if (!Number.isInteger(pacienteId)) {
    return res.status(400).json({ erro: 'pacienteId é obrigatório e deve ser um número inteiro' });
  }
  if (!Number.isInteger(consultaId)) {
    return res.status(400).json({ erro: 'consultaId é obrigatório e deve ser um número inteiro' });
  }
  if (!TIPOS.includes(tipo)) {
    return res.status(400).json({ erro: `tipo inválido (use: ${TIPOS.join(', ')})` });
  }
  if (typeof mensagem !== 'string' || mensagem.trim() === '') {
    return res.status(400).json({ erro: 'mensagem é obrigatória' });
  }

  const notificacao = await Notificacao.create({ pacienteId, consultaId, tipo, mensagem });

  // O "envio" é simulado: só escreve no console
  console.log(`[NOTIFICAÇÃO] paciente ${pacienteId}: ${mensagem}`);

  res.status(201).json(notificacao);
});

// Lista os avisos de um paciente, mais recentes primeiro
app.get('/notificacoes/paciente/:pacienteId', async (req, res) => {
  const pacienteId = Number(req.params.pacienteId);
  if (!Number.isInteger(pacienteId)) {
    return res.status(400).json({ erro: 'pacienteId deve ser um número inteiro' });
  }
  const lista = await Notificacao.find({ pacienteId }).sort({ dataEnvio: -1 });
  res.json(lista);
});

// ---------- Erros sempre no formato {"erro": "..."} ----------

// Rota que não existe
app.use((req, res) => {
  res.status(404).json({ erro: 'Rota não encontrada' });
});

// Qualquer erro lançado nas rotas (inclusive JSON mal formado)
app.use((err, req, res, next) => {
  if (err.type === 'entity.parse.failed') {
    return res.status(400).json({ erro: 'Corpo da requisição deve ser um JSON válido' });
  }
  console.error('Erro inesperado:', err);
  res.status(500).json({ erro: 'Erro interno no servidor' });
});

// ---------- Inicialização ----------

// Conecta no MongoDB primeiro; só depois começa a aceitar requisições
mongoose
  .connect(MONGO_URL)
  .then(() => {
    app.listen(PORTA, () => {
      console.log(`Serviço de Notificações rodando em http://localhost:${PORTA}`);
    });
  })
  .catch((err) => {
    console.error('Não foi possível conectar ao MongoDB:', err.message);
    process.exit(1);
  });
