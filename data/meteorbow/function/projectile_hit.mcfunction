function meteorbow:util/explosion_effect

scoreboard players operation #temp meteorbow.proj_owner = @s meteorbow.proj_owner
execute as @a run tag @s remove meteorbow.owner_temp
execute as @a if score @s meteorbow.owner_id = #temp meteorbow.proj_owner run tag @s add meteorbow.owner_temp

execute as @e[type=#minecraft:living,distance=..6,tag=!meteorbow.owner_temp] run damage @s 8 minecraft:explosion
execute as @e[type=#minecraft:living,distance=..6,tag=!meteorbow.owner_temp] run data merge entity @s {Fire:80}

execute as @a run tag @s remove meteorbow.owner_temp

kill @s
