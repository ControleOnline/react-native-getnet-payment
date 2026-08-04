# @controleonline/react-native-getnet-payment

Módulo React Native para integração de pagamentos com terminais **Getnet POS** via **deeplink**.

Suporta crédito, débito, voucher, PIX, estorno, reimpressão e consulta de status.

> **Apenas Android.** Requer o app de pagamentos da Getnet instalado no terminal.

## Instalação

```bash
npm install @controleonline/react-native-getnet-payment
# ou
yarn add @controleonline/react-native-getnet-payment
```

### Android

1. `minSdkVersion` >= 22

2. Registre o package no `MainApplication.java` / `MainApplication.kt`:

```java
import com.controleonline.pos.getnet.payment.GetnetPackage;

// em getPackages():
packages.add(new GetnetPackage());
```

## Uso

```js
import Getnet from '@controleonline/react-native-getnet-payment';
import { v4 as uuidv4 } from 'uuid';

// Pagamento crédito à vista
const result = await Getnet.payment({
  amount: 15.0,
  paymentType: 'credit', // credit | debit | voucher | pix
  callerId: uuidv4(),
  installments: 1,
  // creditType: 'creditMerchant' | 'creditIssuer', // só se installments > 1
  // allowPrintCurrentTransaction: true,
  // orderId: 'PEDIDO-123',
});

if (result.success) {
  console.log('Aprovado!', result.authorizationCode, result.nsu);
} else {
  console.log('Falhou:', result.result, result.resultDetails);
}

// Estorno
await Getnet.refund({
  amount: 15.0,
  cvNumber: result.cvNumber,
  // transactionDate: '150921',
  // originTerminal: '...',
});

// Reimpressão do último comprovante
await Getnet.reprint();

// Consulta de status
await Getnet.checkStatus(callerId);
```

## Métodos

### `payment(params)`

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `amount` | number | sim | Valor em reais (ex: 15.50) |
| `paymentType` | string | sim | `credit`, `debit`, `voucher` ou `pix` |
| `callerId` | string | sim | Identificador único da transação |
| `installments` | number | não | Parcelas (1–12). Débito deve ser 1 |
| `creditType` | string | não | `creditMerchant` ou `creditIssuer` (parcelado) |
| `allowPrintCurrentTransaction` | boolean | não | Imprimir comprovante |
| `orderId` | string | não | ID do pedido |

### `refund(params)`

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `amount` | number | sim | Valor em reais |
| `cvNumber` | string | não | Número do CV da transação original |
| `transactionDate` | string | não | Data da transação |
| `originTerminal` | string | não | Terminal de origem |
| `allowPrintCurrentTransaction` | boolean | não | Imprimir comprovante |

### `reprint()`

Reimprime o comprovante da última transação.

### `checkStatus(callerId)`

Consulta o status de uma transação pelo `callerId`.

## Resposta

```js
{
  result: "0",              // "0" = sucesso
  resultDetails: "TRANSACAO APROVADA",
  success: true,
  amount: "000000001500",
  callerId: "...",
  nsu: "...",
  cvNumber: "...",
  authorizationCode: "...",
  cardBin: "...",
  cardLastDigits: "...",
  brand: "...",
  installments: "...",
  nsuLocal: "...",
  // ... demais campos retornados pelo app Getnet
}
```

### Códigos de `result`

| Código | Significado |
|--------|-------------|
| `0` | Sucesso |
| `1` | Negada |
| `2` | Cancelada |
| `3` | Falha |
| `4` | Desconhecido |
| `5` | Pendente |

## Deeplinks

| Operação | URI |
|----------|-----|
| Pagamento | `getnet://pagamento/v3/payment?...` |
| Estorno | `getnet://pagamento/v1/refund?...` |
| Reimpressão | `getnet://pagamento/v1/reprint` |
| Status | `getnet://pagamento/v1/checkstatus?callerId=...` |

## Requisitos

- Terminal Getnet (Smart POS) com app de pagamentos instalado
- Android API 22+
- React Native

## Referências

- [Documentação Get Smart](https://getstore.getnet.com.br/)
- [App2App Integration](https://docs.globalgetnet.com/en/products/in-store-payments/app2app-integration)

## License

MIT
