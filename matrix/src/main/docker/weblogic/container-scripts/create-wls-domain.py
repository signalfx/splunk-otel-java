import os

domain_path = os.environ.get("DOMAIN_HOME")
admin_server_name = ADMIN_NAME
admin_port = int(os.environ.get("ADMIN_PORT"))
domain_name = os.environ.get("DOMAIN_NAME")

print('domain_path              : [%s]' % domain_path)
print('domain_name              : [%s]' % domain_name)
print('admin_server_name        : [%s]' % admin_server_name)
print('admin_port               : [%s]' % admin_port)

# Open default domain template
# ============================
readTemplate("/u01/oracle/wlserver/common/templates/wls/wls.jar")

set('Name', domain_name)
setOption('DomainName', domain_name)
create(domain_name, 'Log')
cd('/Log/%s' % domain_name)
set('FileName', '%s.log' % domain_name)

# Configure the Administration Server
# ===================================
cd('/Servers/AdminServer')
set('ListenPort', admin_port)
set('Name', admin_server_name)

# Set the admin user's username and password
# ==========================================
cd('/Security/%s/User/weblogic' % domain_name)
cmo.setName(username)
cmo.setPassword(password)

setOption('OverwriteDomain', 'true')

writeDomain(domain_path)
closeTemplate()

# Update Domain
readDomain(domain_path)
cd('/')

# Allow deployment from autodeploy
cmo.setProductionModeEnabled(false)
updateDomain()
closeDomain()
print('Done')

exit()
