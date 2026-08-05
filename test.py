from cron_converter.cron_conversion import convert_to_cron

cron_expression = convert_to_cron("Every Monday , Wednesday, and Friday at 10:00 AM")

print(cron_expression)