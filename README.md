# Serviço Proxy ID Checker

## Introdução 

API Desenvolvida para consultar dados do cliente na Equifax (provedor de dados) e salvar dados em uma base local.

Nota: 
* Esse é um serviço rest desenvolvido como um bundle para instalação e execução em um container OSGI sobre RedHat Karaf 7.6.
* Afim de faciliar uma migração futura para tecnologias mais modernas como serveless, estamos utilizando puramente Java + libs. 
* Para que o serviço consiga executar no container OSGI utilizamos Apache Camel + Blueprint para envelopar o serviço e injetar os data sources e properties.

## Instalação 

Essa API depende de uma série de bundles já deveriam estar pré instalados no servidor do Karaf.

**Notas gerais sobre a instalação:**

* O karaf faz o download dos pacotes diretamente do repositório maven
* Caso o Karaf não esteja integrado com o repositório na internet, é necessário copiar manualmente os artefatos para a pasta .m2 do usuário que estiver executando o karaf
* Para acessar o painel de comando do karaf va até diretório {raiz_fuse}/bin e execute o ./client
* Para verificar os logs do fuse va até diretório {raiz_fuse}/data/log e você vai encontrar os arquivos fuse.log e http_trace.log
* Após instalação do bundle, verifique se o mesmo está em estado ativo e se não estiver procure o erro nos logs

**Exemplos de comandos de instalação:**

Abaixo exemplos de comandos básicos para instalação e manutenção dos bundles:

```bash
bundle:list
bundle:uninstall 999
bundle:update 999 >>> atualiza o bundle re-fazendo o deploy do artefato que estiver no .m2
bundle:refresh 999 >>> apenas reinicia o bundle
bundle:install -s mvn:com.ahumada/api-xpto/1.0.0
bundle:install -s 'wrap:mvn:com.squareup.okio/okio/1.17.5/$Bundle-SymbolicName=okio&Bundle-Version=1.17.5&Export-Package=*;version="1.17.5"' >>> instala um jar como um bundle exportando uma versão especifica
```
*Lembrando que o número 999 é um exemplo e deve ser substituido pelo número do bundle que você quiser manipular*

**1) Bundles Base**

Antes de instalar, verifique se esses bundles já não estão presentes no servidor através do comando bundle:list.

```bash
bundle:install -s 'wrap:mvn:com.fasterxml.jackson.jr/jackson-jr-objects/2.8.11'
bundle:install -s 'wrap:mvn:log4j/log4j/1.2.17/$Bundle-SymbolicName=log4j&Bundle-Version=1.2.17&Export-Package=*;version="1.2.17"'
bundle:install -s 'wrap:mvn:com.squareup.okio/okio/1.17.5/$Bundle-SymbolicName=okio&Bundle-Version=1.17.5&Export-Package=*;version="1.17.5"'
bundle:install -s 'wrap:mvn:com.squareup.okhttp3/okhttp/3.14.9/$Bundle-SymbolicName=okhttp&Bundle-Version=3.14.9&Export-Package=*;version="3.14.9"'
bundle:install -s 'wrap:mvn:com.fasterxml.jackson.datatype/jackson-datatype-jsr310/2.9.10/$Bundle-SymbolicName=jackson-datatype-jsr310&Bundle-Version=2.9.10&Export-Package=*;version="2.9.10"'
bundle:install -s 'wrap:mvn:com.tvf.ws/rest-client/1.1.1/$Bundle-SymbolicName=rest-client&Bundle-Version=1.1.1&Export-Package=*;version="1.1.1"&Import-Package=okio,okhttp3,*'
bundle:install -s 'wrap:mvn:com.tvf.db/db-helper/1.0.2/$Bundle-SymbolicName=db-helper&Bundle-Version=1.0.2&Export-Package=*;version="1.0.2"'
bundle:install -s 'wrap:mvn:com.microsoft.sqlserver/mssql-jdbc/9.4.0.jre8'
bundle:install -s 'wrap:mvn:com.oracle.database.jdbc/ojdbc6/11.2.0.4'
```

**2) Bundle do serviço**

Lembrar de verficar a versão atual antes de instalar. 


```bash
bundle:install -s mvn:com.ahumada/proxy-id-checker/1.0.0
```