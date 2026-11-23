# Serviço de Pacientes — FastAPI na porta 8001 (ou na porta da variável PORT).
# Rodar local: uvicorn main:app --port 8001 --reload
# No Railway: uvicorn main:app --host 0.0.0.0 --port $PORT (ver railway.json)
import os
from datetime import date

from fastapi import Depends, FastAPI, HTTPException, Request, Response
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy import or_
from sqlalchemy.orm import Session

from cpf import cpf_valido
from database import Base, SessionLocal, engine, get_db
from modelos import Paciente
from schemas import PacienteEntrada, PacienteSaida

app = FastAPI(title="Serviço de Pacientes")

# CORS: libera o frontend React local (localhost:3000) e o frontend publicado (variável FRONTEND_URL)
ORIGENS_PERMITIDAS = ["http://localhost:3000"]
if os.getenv("FRONTEND_URL"):
    ORIGENS_PERMITIDAS.append(os.getenv("FRONTEND_URL").rstrip("/"))

app.add_middleware(
    CORSMiddleware,
    allow_origins=ORIGENS_PERMITIDAS,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------- Erros sempre no formato {"erro": "..."} ----------

@app.exception_handler(HTTPException)
def tratar_http_exception(request: Request, exc: HTTPException):
    return JSONResponse(status_code=exc.status_code, content={"erro": exc.detail})


@app.exception_handler(RequestValidationError)
def tratar_validacao(request: Request, exc: RequestValidationError):
    # JSON com campo faltando ou no formato errado -> 400
    primeiro = exc.errors()[0]
    campo = primeiro["loc"][-1]
    if primeiro["type"] == "json_invalid" or campo == "body":
        return JSONResponse(status_code=400, content={"erro": "Corpo da requisição deve ser um JSON válido"})
    return JSONResponse(status_code=400, content={"erro": f"Campo inválido ou ausente: {campo}"})


# ---------- Criação das tabelas e dados de exemplo ----------

def popular_exemplos():
    """Cadastra 3 pacientes de exemplo se a tabela estiver vazia."""
    db = SessionLocal()
    try:
        if db.query(Paciente).count() == 0:
            db.add_all([
                Paciente(nome="Ana Souza", cpf="12345678909", data_nascimento=date(1995, 4, 10),
                         telefone="85999990000", email="ana@email.com"),
                Paciente(nome="Bruno Costa", cpf="11144477735", data_nascimento=date(1988, 9, 23),
                         telefone="85988880000", email="bruno@email.com"),
                Paciente(nome="Carla Mendes", cpf="39053344705", data_nascimento=date(2001, 1, 5),
                         telefone="85977770000", email="carla@email.com"),
            ])
            db.commit()
    finally:
        db.close()


@app.on_event("startup")
def ao_iniciar():
    Base.metadata.create_all(bind=engine)  # cria a tabela se não existir
    popular_exemplos()


# ---------- Funções auxiliares ----------

def buscar_ou_404(db: Session, paciente_id: int) -> Paciente:
    paciente = db.get(Paciente, paciente_id)
    if paciente is None:
        raise HTTPException(status_code=404, detail="Paciente não encontrado")
    return paciente


def validar_dados(db: Session, dados: PacienteEntrada, id_atual: int | None = None):
    """Regras: nome obrigatório, CPF válido e CPF único."""
    if not dados.nome.strip():
        raise HTTPException(status_code=400, detail="Nome é obrigatório")
    if not cpf_valido(dados.cpf):
        raise HTTPException(status_code=400, detail="CPF inválido")
    outro = db.query(Paciente).filter(Paciente.cpf == dados.cpf).first()
    if outro is not None and outro.id != id_atual:
        raise HTTPException(status_code=409, detail="CPF já cadastrado")


# ---------- Rotas ----------

@app.get("/health")
def health():
    return {"status": "ok", "servico": "pacientes"}


@app.post("/pacientes", response_model=PacienteSaida, status_code=201)
def cadastrar(dados: PacienteEntrada, db: Session = Depends(get_db)):
    validar_dados(db, dados)
    paciente = Paciente(**dados.model_dump(), ativo=True)
    db.add(paciente)
    db.commit()
    db.refresh(paciente)
    return paciente


@app.get("/pacientes", response_model=list[PacienteSaida])
def listar(busca: str | None = None, db: Session = Depends(get_db)):
    consulta = db.query(Paciente)
    if busca:
        # filtra por parte do nome ou do CPF
        consulta = consulta.filter(or_(Paciente.nome.ilike(f"%{busca}%"), Paciente.cpf.contains(busca)))
    return consulta.order_by(Paciente.nome).all()


@app.get("/pacientes/{paciente_id}", response_model=PacienteSaida)
def detalhar(paciente_id: int, db: Session = Depends(get_db)):
    return buscar_ou_404(db, paciente_id)


@app.put("/pacientes/{paciente_id}", response_model=PacienteSaida)
def atualizar(paciente_id: int, dados: PacienteEntrada, db: Session = Depends(get_db)):
    paciente = buscar_ou_404(db, paciente_id)
    validar_dados(db, dados, id_atual=paciente_id)
    for campo, valor in dados.model_dump().items():
        setattr(paciente, campo, valor)
    db.commit()
    db.refresh(paciente)
    return paciente


@app.delete("/pacientes/{paciente_id}", status_code=204)
def desativar(paciente_id: int, db: Session = Depends(get_db)):
    # Não apaga: só marca como inativo (o histórico de consultas continua fazendo sentido)
    paciente = buscar_ou_404(db, paciente_id)
    paciente.ativo = False
    db.commit()
    return Response(status_code=204)


# Permite rodar também com "python main.py" (escuta em 0.0.0.0 na porta da variável PORT)
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", "8001")))
