execute as @a if score @s meteorbow.used > @s meteorbow.used_prev if items entity @s weapon.mainhand minecraft:crossbow[minecraft:custom_data={meteorbow:1b}] run function meteorbow:shoot_check

scoreboard players remove @a[scores={meteorbow.cooldown=1..}] meteorbow.cooldown 1

execute as @e[tag=meteorbow.projectile] at @s run function meteorbow:projectile_tick

execute as @a run scoreboard players operation @s meteorbow.used_prev = @s meteorbow.used
