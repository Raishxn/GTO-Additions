# Entangled Miner: referência e implementação

Referência estudada: [EasyTechnology](https://github.com/liansishen/EasyTechnology),
revisão `aef7d1edfb3c7ec17e9be3d165480c191d3f881a`, para GTNH/Minecraft 1.7.10.
Nesta revisão não há uma classe chamada Entangled Miner. As ideias próximas são
`ETHVoidOilLocationCard` (cartão de campo remoto), `ETHOilDrillMiner` (uso do cartão)
e `ETHVoidMinerBase` (produção virtual com pesos de minérios de uma dimensão).

O GTO-Additions implementa código novo para as APIs do dev9, seguindo o comportamento
pedido: produção infinita de **raw ores de um depósito específico**, sem extração ou
esgotamento. Não copia a implementação de mineração remota ou de tickets de chunks.

O cartão consulta `ServerCache.getNearbyVeins` após inicializar o cache da dimensão.
`GeneratedVeinMetadata` fornece ID, centro e definição do depósito gerado. A vinculação
seleciona o depósito mais próximo cuja composição contém o material do bloco clicado.
O cartão guarda dimensão, ID e centro em NBT, sem modificar os metadados do depósito.

A máquina resolve `GTRegistries.ORE_VEINS` e lê `veinGenerator().getAllEntries()`.
Cada material é convertido pelo `ChemicalHelper` para `TagPrefix.rawOre`; materiais sem
raw ore são descartados, e entradas iguais têm seus pesos somados. Um seletor ponderado
usa a identidade do vínculo e o contador persistido de ciclos concluídos. O cartão funciona
como catalisador não consumido. As máquinas são singleblocks Steam–EV: 4/8/16/32/64 raw ores por segundo.
Os lotes de 5 ticks geram 1/2/4/8/16 ores. Steam usa carvão; LV–EV consomem V[tier].

Uma `RecipeLogic` própria verifica espaço novamente antes da conclusão. No dev9, retornar
`true` de `onRecipeFinish` mantém o lote pendente; portanto uma saída bloqueada não entrega
parcialmente o lote nem executa `afterWorking`, e não cobra mais energia enquanto espera.
A transformação do registro de recipe types ocorre no início de `GTRecipeTypes.init`, antes
do fechamento do registro. O controlador segue o registro precoce já usado pelo addon.

Não há chamada a `OreGenerator`, depleção, quebra de blocos ou carregamento de chunk remoto.
A área original pode ser descarregada após criar o vínculo. Depósitos de bedrock ore não são vinculados pelo cartão de ores.

O Entangled Oil Drill também está implementado, em Steam–EV. Seu cartão consulta localmente
`BedrockFluidVeinSavedData.getFluidVeinWorldEntry` para identificar o campo e o rendimento
original. Essa sondagem pode inicializar o registro nativo do campo; não altera as operações
restantes e não chama `depleteVein`. Guarda dimensão, ID do campo, fluido, chunk e rendimento.
A produção só resolve a definição registrada e produz rendimento × 1/2/4/8/16 a cada 20 ticks;
não consulta chunks remotos. Gera petróleo, gás ou o outro fluido daquele campo, infinitamente.
As famílias compartilham combustível Steam, inventários internos, UI e proteção de saída.
Instruções de craft e uso estão no README.

Compilação e testes automatizados aprovados; interfaces, funcionamento e persistência das máquinas ainda
precisam de validação dentro do dev9.
