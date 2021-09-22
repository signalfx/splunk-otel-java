FROM mcr.microsoft.com/windows/servercore:ltsc2019
ADD https://github.com/open-telemetry/opentelemetry-collector-contrib/releases/latest/download/otelcontribcol_windows_amd64.exe otelcol_windows_amd64.exe
ENV NO_WINDOWS_SERVICE=1
ENTRYPOINT /otelcol_windows_amd64.exe
