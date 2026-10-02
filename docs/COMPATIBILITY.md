# Compatibilidade e atualização

A versão `0.1.0-dev9` do GTO-Additions tem como alvo **GregTech Odyssey 0.6.0-dev9**,
Minecraft **1.20.1**, Forge **47.4.20** e Java **21 ou superior**.

## Dependências

GTOCore/GTOLib **26.9.5** e o fork do GTCEu **26.9.70** são obrigatórios e fixados
nos metadados. AE2 **15.269.3**, LDLib **1.0.52.a** e DataSyncLib **26.9.4** são as
APIs usadas da instalação dev9. O script de preparação extrai os jars internos
GTCEu e AE2 do GTOCore e copia as demais APIs para compilação.

O addon utiliza coremods para o registro antecipado das máquinas e integrações
com receitas/estruturas. Atualizações dessas dependências podem alterar os pontos
de integração. Compatibilidade com outras versões do GTO ou com GTCEu padrão
não foi validada. Fabric e NeoForge não são alvos deste build Forge.

Em multiplayer, use o addon e as dependências correspondentes nos dois lados.

## Atualizar o addon

Feche o jogo/servidor, guarde uma cópia do mundo e substitua o jar antigo na pasta
`mods`. Não deixe duas versões do addon instaladas ao mesmo tempo.

O GTO mantém caches persistentes de recursos e receitas. O instalador do projeto
faz backup do jar anterior, substitui-o e invalida somente estes arquivos em
`gtocore/cache/`:

- `gto-additions-0.1.0-dev9.jar.bin`
- `resources` e `resource_exist`
- `json/recipes`
- `tags/recipe_serializer` e `tags/recipe_type`

Eles serão reconstruídos ao iniciar. Não remova toda a pasta `gtocore`, pois ela
pode conter outros dados do modpack. O backup do instalador fica em
`build/previous/install-<data-hora>/` dentro do projeto.

## Configuração da Universal Factory

O arquivo `config/gtoa/balance/universal_factory.json` é criado no primeiro uso.
Reinicie o jogo/servidor após editar o arquivo. A configuração do servidor controla
a execução das máquinas. Valores padrão e modos estão no [guia de recursos](FEATURES.md).

A estrutura exige saída adequada para os itens/fluidos produzidos. O Pattern
Buffer de entrada não elimina a necessidade de saída. Ao esvaziar a fila, a máquina
fica ociosa e continua verificando novas receitas.

## Limites da validação

55 testes automatizados aprovados e Universal Factory confirmada em jogo pelo
mantenedor no dev9. A validação não cobre todos os recursos, combinações de hatches,
condições especiais, persistência de lotes em andamento ou servidores dedicados.
Ao relatar um problema, inclua versões, logs, receita e montagem da máquina.
