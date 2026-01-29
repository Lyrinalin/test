execute as @a[scores={fb_bowUsed=1..}] if data entity @s SelectedItem.components."minecraft:custom_data"{firebow:1b} if entity @s[gamemode=creative] at @s run tag @e[type=arrow,distance=..2,sort=nearest,limit=1,tag=!meteor_carrier] add meteor_carrier
execute as @a[scores={fb_bowUsed=1..}] if data entity @s SelectedItem.components."minecraft:custom_data"{firebow:1b} if entity @s[gamemode=creative] at @s run data merge entity @e[type=arrow,distance=..2,sort=nearest,limit=1,tag=meteor_carrier,tag=!meteor_init] {pickup:0b,damage:6.0d,Fire:60s}

execute as @a[scores={fb_bowUsed=1..}] if data entity @s SelectedItem.components."minecraft:custom_data"{firebow:1b} if entity @s[gamemode=!creative,nbt={Inventory:[{id:"minecraft:magma_block"}]}] at @s run clear @s minecraft:magma_block 1
execute as @a[scores={fb_bowUsed=1..}] if data entity @s SelectedItem.components."minecraft:custom_data"{firebow:1b} if entity @s[gamemode=!creative,nbt={Inventory:[{id:"minecraft:magma_block"}]}] at @s run tag @e[type=arrow,distance=..2,sort=nearest,limit=1,tag=!meteor_carrier] add meteor_carrier
execute as @a[scores={fb_bowUsed=1..}] if data entity @s SelectedItem.components."minecraft:custom_data"{firebow:1b} if entity @s[gamemode=!creative,nbt={Inventory:[{id:"minecraft:magma_block"}]}] at @s run data merge entity @e[type=arrow,distance=..2,sort=nearest,limit=1,tag=meteor_carrier,tag=!meteor_init] {pickup:0b,damage:6.0d,Fire:60s}

execute as @a[scores={fb_bowUsed=1..}] if data entity @s SelectedItem.components."minecraft:custom_data"{firebow:1b} if entity @s[gamemode=!creative] unless entity @s[nbt={Inventory:[{id:"minecraft:magma_block"}]}] at @s run kill @e[type=arrow,distance=..2,sort=nearest,limit=1,tag=!meteor_carrier]
execute as @a[scores={fb_bowUsed=1..}] if data entity @s SelectedItem.components."minecraft:custom_data"{firebow:1b} if entity @s[gamemode=!creative] unless entity @s[nbt={Inventory:[{id:"minecraft:magma_block"}]}] at @s run playsound minecraft:block.dispenser.fail player @s ~ ~ ~ 0.7 1.2

scoreboard players set @a[scores={fb_bowUsed=1..}] fb_bowUsed 0
