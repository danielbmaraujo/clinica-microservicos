# Conexão com o banco MySQL do Serviço de Pacientes.
# Os dados de conexão vêm de variáveis de ambiente (com valor padrão para rodar local).
import os

from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "3308")
DB_USER = os.getenv("DB_USER", "clinica")
DB_PASSWORD = os.getenv("DB_PASSWORD", "clinica123")
DB_NAME = os.getenv("DB_NAME", "db_pacientes")

URL_BANCO = f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# pool_pre_ping: testa a conexão antes de usar (evita erro se o MySQL reiniciar)
engine = create_engine(URL_BANCO, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine)
Base = declarative_base()


def get_db():
    """Abre uma sessão com o banco por requisição e fecha no final."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
