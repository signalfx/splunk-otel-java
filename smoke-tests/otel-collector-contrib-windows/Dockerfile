FROM mcr.microsoft.com/windows/servercore:ltsc2019
ADD https://github.com/signalfx/splunk-otel-collector/releases/latest/download/otelcol_windows_amd64.exe otelcol_windows_amd64.exe
ENV NO_WINDOWS_SERVICE=1
ENTRYPOINT /otelcol_windows_amd64.exe
