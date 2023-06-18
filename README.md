# Naspter Service Java

Serviço inspirado no conceito de arquitetura do Napster para a disciplina de 
Sistemas Distribuídos da UFABC no 2o quadrimestre de 2023. O serviço é escrito em Java
e utiliza RMI e comunicação TCP para implementação do serviço.

## Como utilizar

- Compilar os arquivos do projeto utilizando Java 8
- Executar uma instância do Servidor na classe `server.ServerImpl`
- Executar quantas instâncias se desejar de peers, na classe `peer.PeerImpl`
- Cada peer deve primeiro inicializar no servidor fornecendo IP, porta e pasta 
de onde serão armazenados e carregados os arquivos
- Agora, podemos executar uma das operações que são
  - Update: atualização de um arquivo adicionado a pasta;
  - Search: busca de um arquivo disponível por Peers no servidor
  - Download: download de um arquivo diretamente de um Peer
