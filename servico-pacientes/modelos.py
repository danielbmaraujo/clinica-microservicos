# Tabela "pacientes" no banco (mapeamento do SQLAlchemy).
from sqlalchemy import Boolean, Column, Date, Integer, String

from database import Base


class Paciente(Base):
    __tablename__ = "pacientes"

    id = Column(Integer, primary_key=True, autoincrement=True)
    nome = Column(String(100), nullable=False)
    cpf = Column(String(11), nullable=False, unique=True)  # CPF único
    data_nascimento = Column(Date, nullable=True)
    telefone = Column(String(20), nullable=True)
    email = Column(String(100), nullable=True)
    ativo = Column(Boolean, nullable=False, default=True)  # DELETE só desativa
