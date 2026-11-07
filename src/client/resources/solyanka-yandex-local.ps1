# SPDX-License-Identifier: GPL-3.0-only
# Windows SMTC: https://learn.microsoft.com/en-us/uwp/api/windows.media.control.globalsystemmediatransportcontrolssession
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime] | Out-Null
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
function Await($operation, $resultType) {
    $task = $asTaskGeneric.MakeGenericMethod($resultType).Invoke($null, @($operation))
    if (-not $task.Wait(5000)) { throw 'Windows media request timed out' }
    return $task.Result
}
$manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
$lastKey = ''; $art = ''; $artAttempt = [DateTime]::MinValue
$maxArtBytes = 1048576
# shortcut: polling every two seconds, upgrade to event subscriptions after validating the Windows prototype.
while (Get-Process -Id __PARENT_PID__ -ErrorAction SilentlyContinue) {
    try {
        $session = $manager.GetSessions() | Where-Object { [IO.Path]::GetFileName($_.SourceAppUserModelId) -in @('Яндекс Музыка.exe', 'Yandex Music.exe') } | Select-Object -First 1
        if ($null -eq $session) {
            $result = @{state='waiting'}; $lastKey = ''; $art = ''
        } else {
            $props = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
            $title = [string]$props.Title; $artist = [string]$props.Artist
            if ($title.Length -gt 300) { $title = $title.Substring(0,300) }
            if ($artist.Length -gt 300) { $artist = $artist.Substring(0,300) }
            $key = $title + "`n" + $artist
            if ($key -ne $lastKey) {
                $lastKey = $key; $art = ''; $artAttempt = [DateTime]::MinValue
                # Publish the basic metadata before attempting any optional artwork or timeline APIs.
                [Console]::Out.WriteLine((@{state='connected';title=$title;artist=$artist} | ConvertTo-Json -Compress))
                [Console]::Out.Flush()
            }
            # Keep enough room for normal Yandex artwork. The Kotlin reader and JSON line
            # limit are matched to this bound; larger artwork is skipped safely.
            $thumbnail = $null
            try { $thumbnail = $props.Thumbnail } catch {}
            if (-not $art -and $null -ne $thumbnail -and ([DateTime]::UtcNow - $artAttempt).TotalSeconds -ge 30) {
                $artAttempt = [DateTime]::UtcNow
                $stream = $null; $reader = $null
                try {
                    [Windows.Storage.Streams.DataReader, Windows.Storage.Streams, ContentType=WindowsRuntime] | Out-Null
                    [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType=WindowsRuntime] | Out-Null
                    $stream = Await ($thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                    if ($stream.Size -gt 0 -and $stream.Size -le $maxArtBytes) {
                        $reader = [Windows.Storage.Streams.DataReader]::new($stream.GetInputStreamAt(0))
                        $bytes = New-Object byte[] ([int]$stream.Size)
                        $offset = 0
                        while ($offset -lt $bytes.Length) {
                            $request = [uint32][Math]::Min(65536, $bytes.Length - $offset)
                            $loaded = Await ($reader.LoadAsync($request)) ([uint32])
                            if ($loaded -eq 0) { throw 'Thumbnail stream ended early' }
                            $chunk = New-Object byte[] $loaded
                            $reader.ReadBytes($chunk)
                            [Array]::Copy($chunk, 0, $bytes, $offset, [int]$loaded)
                            $offset += [int]$loaded
                        }
                        $art = [Convert]::ToBase64String($bytes)
                    }
                } catch {} finally {
                    try { if ($reader) { $reader.Dispose() } } catch {}
                    try { if ($stream) { $stream.Dispose() } } catch {}
                }
            }
            $position = 0; $duration = 0; $playing = $false
            try { $playing = $session.GetPlaybackInfo().PlaybackStatus.ToString() -eq 'Playing' } catch {}
            try {
                $timeline = $session.GetTimelineProperties()
                $position = $timeline.Position.TotalSeconds; $duration = $timeline.EndTime.TotalSeconds
                if ($playing -and $duration -gt 0) {
                    $age = ([DateTimeOffset]::UtcNow - $timeline.LastUpdatedTime).TotalSeconds
                    if ($age -ge 0 -and $age -le $duration) { $position += $age }
                }
            } catch {}
            $result = @{state='connected';title=$title;artist=$artist;art=$art;position=$position;duration=$duration;playing=$playing}
        }
    } catch { $result = @{state='error'}; $lastKey = ''; $art = '' }
    [Console]::Out.WriteLine(($result | ConvertTo-Json -Compress))
    [Console]::Out.Flush()
    Start-Sleep -Seconds 2
}
