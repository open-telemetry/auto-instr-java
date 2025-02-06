ARG jdkImage

# Unzip in a separate container so that zip file layer is not part of final image
FROM mcr.microsoft.com/windows/servercore:ltsc2022@sha256:a7faef3c463f53996b00abff1cad6e979c75230bf9a55fdce77fe99c764971a5 as builder
ARG version

ADD https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/${version}/payara-${version}.zip /server.zip
RUN ["powershell", "-Command", "expand-archive -Path /server.zip -DestinationPath /server"]
RUN ["powershell", "-Command", "Get-ChildItem -Path /server/ -filter payara* | Rename-Item -NewName payara"]
RUN ["powershell", "-Command", "remove-item -Path /server/payara/glassfish/modules/phonehome-bootstrap.jar"]

FROM ${jdkImage}-windowsservercore-ltsc2022

# Make /server the base directory to simplify all further paths
COPY --from=builder /server/payara /server
COPY app.war /server/glassfish/domains/domain1/autodeploy/
COPY launch.bat /server/
WORKDIR /server
CMD /server/launch.bat
