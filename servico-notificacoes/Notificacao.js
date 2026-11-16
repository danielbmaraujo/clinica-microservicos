// Modelo da coleção "notificacoes" no MongoDB (Mongoose).
const mongoose = require('mongoose');

const TIPOS = ['CONFIRMACAO', 'REMARCACAO', 'CANCELAMENTO', 'LEMBRETE'];

const notificacaoSchema = new mongoose.Schema({
  pacienteId: { type: Number, required: true },
  consultaId: { type: Number, required: true },
  tipo: { type: String, required: true, enum: TIPOS },
  mensagem: { type: String, required: true },
  dataEnvio: { type: Date, default: Date.now },
});

// Formato do JSON devolvido pela API:
// troca "_id" por "id", remove "__v" e mostra a data como "2026-11-16T15:30:00Z"
notificacaoSchema.set('toJSON', {
  transform: (doc, ret) => ({
    id: ret._id.toString(),
    pacienteId: ret.pacienteId,
    consultaId: ret.consultaId,
    tipo: ret.tipo,
    mensagem: ret.mensagem,
    dataEnvio: ret.dataEnvio.toISOString().replace(/\.\d{3}Z$/, 'Z'),
  }),
});

const Notificacao = mongoose.model('Notificacao', notificacaoSchema, 'notificacoes');

module.exports = { Notificacao, TIPOS };
