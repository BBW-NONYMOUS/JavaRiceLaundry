param([switch]$IncludeDatabaseScreens)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    & mvn.cmd -q compile dependency:build-classpath '-Dmdep.outputFile=target/dashboard-classpath.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Maven compilation failed.' }
    $dashboardDependencies = (Get-Content target/dashboard-classpath.txt -Raw).Trim()
    $dashboardClasspath = "target/classes;$dashboardDependencies"
    New-Item -ItemType Directory -Path target/checks -Force | Out-Null
    & javac --release 17 -encoding UTF-8 -cp $dashboardClasspath -d target/checks checks/DashboardChecks.java checks/DashboardDatabaseChecks.java checks/ScreenThemeChecks.java
    if ($LASTEXITCODE -ne 0) { throw 'Dashboard check compilation failed.' }
    if ($IncludeDatabaseScreens) {
        & java -cp "target/checks;$dashboardClasspath" DashboardDatabaseChecks
        if ($LASTEXITCODE -ne 0) { throw 'Read-only database check failed.' }
        & java -cp "target/checks;$dashboardClasspath" laundry.ScreenThemeChecks
        if ($LASTEXITCODE -ne 0) { throw 'Screen theme checks failed.' }
    }
    & java -cp "target/checks;$dashboardClasspath" DashboardChecks
    if ($LASTEXITCODE -ne 0) { throw 'Dashboard checks failed.' }
} finally {
    Pop-Location
}
