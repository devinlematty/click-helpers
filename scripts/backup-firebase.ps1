# Backs up the community loadouts database (loadouts, bans and settings) to a
# dated JSON file and keeps the newest 12. Uses the Firebase CLI login saved on
# this PC. Run by the "Click Helpers Firebase backup" scheduled task.
$ErrorActionPreference = 'Stop'

$project = 'click-helpers-loadouts-2b22f'
$folder = Join-Path $env:USERPROFILE '.click-helpers-backups'
$keep = 12

New-Item -ItemType Directory -Force $folder | Out-Null
$file = Join-Path $folder ("loadouts-{0}.json" -f (Get-Date -Format 'yyyy-MM-dd'))
$log = Join-Path $folder 'backup.log'

try
{
	& firebase database:get / --project $project --output $file 2>&1 | Out-Null
	if ($LASTEXITCODE -ne 0 -or -not (Test-Path $file))
	{
		throw "firebase database:get failed with exit code $LASTEXITCODE"
	}
	Add-Content $log ("{0} OK {1} ({2} bytes)" -f (Get-Date -Format s), $file, (Get-Item $file).Length)

	Get-ChildItem $folder -Filter 'loadouts-*.json' |
		Sort-Object Name -Descending |
		Select-Object -Skip $keep |
		Remove-Item -Confirm:$false
}
catch
{
	Add-Content $log ("{0} FAILED {1}" -f (Get-Date -Format s), $_.Exception.Message)
	exit 1
}
