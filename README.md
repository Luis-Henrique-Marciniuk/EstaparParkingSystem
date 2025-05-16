Documentação da API REST do EstaparEsta documentação descreve a API REST para o sistema de gestão de estacionamentos desenvolvido para o Teste Desenvolvedor Java/Kotlin Backend da Estapar. A API permite consultar o status de veículos e vagas, além de obter informações sobre o faturamento do estacionamento.Formato da DocumentaçãoA documentação está no formato OpenAPI 3.0.EndpointsConsulta de PlacaEndpoint: POST /plate-statusDescrição: Retorna o status de um veículo com base na placa.Requisição:{
  "license_plate": "ZUL0001"
}
Resposta:{
  "license_plate": "ZUL0001",
  "price_until_now": 0.00,
  "entry_time": "2025-01-01T12:00:00.000Z",
  "time_parked": "00:00:00"
}
license_plate: Placa do veículo.price_until_now: Preço cobrado até o momento.entry_time: Timestamp de entrada do veículo.time_parked: Tempo desde a entrada do veículo no formato HH:mm:ss.Consulta de VagaEndpoint: POST /spot-statusDescrição: Retorna o status de uma vaga com base em sua localização (latitude e longitude).Requisição:{
  "lat": -23.561684,
  "lng": -46.655981
}
Resposta:{
  "occupied": false,
  "entry_time": "2025-01-01T12:00:00.000Z",
  "time_parked": "00:00:00"
}
occupied: Indica se a vaga está ocupada (true) ou livre (false).entry_time: Timestamp de entrada do veículo na vaga, se estiver ocupada.time_parked: Tempo desde a entrada do veículo na vaga no formato HH:mm:ss.Consulta de FaturamentoEndpoint: GET /revenueDescrição: Retorna o faturamento do estacionamento para uma data e setor específicos.Requisição:{
  "date": "2025-01-01",
  "sector": "A"
}
Resposta:{
  "amount": 0.00,
  "currency": "BRL",
  "timestamp": "2025-01-01T12:00:00.000Z"
}
amount: Valor do faturamento.currency: Moeda (BRL - Real Brasileiro).timestamp: Timestamp da consulta.
