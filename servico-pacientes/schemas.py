# Formato do JSON que entra e sai da API (Pydantic).
# O alias_generator converte os nomes para camelCase no JSON:
#   data_nascimento (Python) <-> dataNascimento (JSON)
from datetime import date
from typing import Optional

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class PacienteEntrada(BaseModel):
    """JSON recebido no POST e no PUT (sem id e sem ativo)."""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    nome: str
    cpf: str
    data_nascimento: Optional[date] = None
    telefone: Optional[str] = None
    email: Optional[str] = None


class PacienteSaida(PacienteEntrada):
    """JSON devolvido pela API."""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True, from_attributes=True)

    id: int
    ativo: bool
