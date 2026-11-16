# Validação de CPF: 11 dígitos numéricos + dígitos verificadores corretos.


def cpf_valido(cpf: str) -> bool:
    # Precisa ter exatamente 11 dígitos numéricos
    if len(cpf) != 11 or not cpf.isdigit():
        return False

    # CPFs com todos os dígitos iguais (111.111.111-11) passam na conta, mas são inválidos
    if cpf == cpf[0] * 11:
        return False

    # Calcula os dois dígitos verificadores e compara com os informados
    for posicao in (9, 10):
        soma = sum(int(cpf[i]) * (posicao + 1 - i) for i in range(posicao))
        digito = (soma * 10) % 11
        if digito == 10:
            digito = 0
        if digito != int(cpf[posicao]):
            return False

    return True
