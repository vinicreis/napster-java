# Naspter Service Java

Serviço inspirado no conceito de arquitetura do Napster para a disciplina de 
Sistemas Distribuídos da UFABC no 2o quadrimestre de 2023. O serviço é escrito em Java
e utiliza RMI e comunicação TCP para implementação do serviço.

## Como utilizar

- Executar uma instância do Servidor no arquivo `server.ServerImpl.java`
- Executar quantas instâncias se desejar de peers, no arquivo `peer.PeerImpl.java`
- Cada peer deve primeiro inicializar no servidor fornecendo IP, porta e pasta 
de onde serão armazenados e carregados os arquivos
- Agora, podemos executar uma das operações que são
  - Search: 
  - Download:
  - Update:
