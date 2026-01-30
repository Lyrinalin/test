execute if score @s meteorbow.cooldown matches 1.. run title @s actionbar {"text":"Огненный лук перезаряжается","color":"red"}
execute if score @s meteorbow.cooldown matches 1.. run return 0

execute unless items entity @s inventory.* minecraft:magma_block run title @s actionbar {"text":"Нужен магмовый блок для выстрела","color":"yellow"}
execute unless items entity @s inventory.* minecraft:magma_block run return 0

execute unless score @s meteorbow.owner_id matches 1.. run scoreboard players operation @s meteorbow.owner_id = #global meteorbow.next_id
execute unless score @s meteorbow.owner_id matches 1.. run scoreboard players add #global meteorbow.next_id 1

function meteorbow:util/consume_ammo

scoreboard players set @s meteorbow.cooldown 10

execute at @s run summon minecraft:marker ^ ^ ^1.2 {Tags:["meteorbow.projectile"]}
execute at @s run scoreboard players set @e[tag=meteorbow.projectile,sort=nearest,limit=1] meteorbow.proj_life 0
execute at @s run scoreboard players operation @e[tag=meteorbow.projectile,sort=nearest,limit=1] meteorbow.proj_owner = @s meteorbow.owner_id
