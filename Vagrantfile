box             = 'centos7.2'
url             = 'https://install.codenvycorp.com/centos7.2.box'

ram             = '4096'
cpus            = '2'
#bridge          = 'eth0'

# Set to "<proto>://<user>:<pass>@<host>:<port>"
http_proxy      = ENV['HTTP_PROXY'] || ENV['http_proxy'] || ""
https_proxy     = ENV['HTTPS_PROXY'] || ENV['https_proxy'] || ""
no_proxy        = ENV['NO_PROXY'] || ENV['no_proxy'] || "codenvy.onprem,localhost,127.0.0.1"

ip              = ENV['CODENVY_IP'] || "192.168.56.110"

codenvy_url     = "http://start.codenvy.com/install-codenvy"
codenvy_options = "--suppress --silent --license=accept"

Vagrant.configure("2") do |config|
  puts ("CODENVY: VAGRANT INSTALLER")
  puts ("CODENVY: REQUIRED: VIRTUALBOX 5.x")
  puts ("CODENVY: REQUIRED: VAGRANT 1.8.x")
  puts ("")
  if (http_proxy.to_s != '' || https_proxy.to_s != '') && !Vagrant.has_plugin?("vagrant-proxyconf")
    puts ("You configured a proxy, but Vagrant's proxy plugin not detected.")
    puts ("Install the plugin with: vagrant plugin install vagrant-proxyconf")
    Process.kill 9, Process.pid
  end

  if Vagrant.has_plugin?("vagrant-proxyconf")
    config.proxy.http     = http_proxy
    config.proxy.https    = https_proxy
    config.proxy.no_proxy = no_proxy
  end

  config.vm.box                   = box
  config.vm.box_url               = url
  config.vm.box_download_insecure = true
  config.ssh.insert_key           = false
  
  if ip.to_s.downcase == "dhcp"
    config.vm.network :private_network, type: "dhcp"
  else
    config.vm.network :private_network, ip: ip
  end
  config.vm.network "forwarded_port", guest: 5005, host: 5005

  config.vm.provider :virtualbox do |vbox|
    vbox.customize [
        'modifyvm', :id,
        '--memory', ram,
        '--cpus', cpus
    ]
    vbox.name = "codenvy-enterprise"
  end

  #Adding hosts rules
  config.vm.provision "shell", inline: "echo -e \"127.0.0.1 localhost\" > /etc/hosts"
  config.vm.provision "shell", inline: "echo -e \"nameserver 8.8.8.8\n\" >> /etc/resolv.conf"

  $script = <<-SHELL
    HTTP_PROXY=$1
    HTTPS_PROXY=$2
    NO_PROXY=$3
    CODENVY_URL=$4
    CODENVY_OPTIONS=$5
    IP=$6

    if [ -n "$HTTP_PROXY" ] || [ -n "$HTTPS_PROXY" ]; then
      echo "."
      echo "."
      echo "CODENVY: CONFIGURING PROXY"
      echo "."
      echo "."
      echo "HTTP PROXY set to: $HTTP_PROXY"
      echo "HTTPS PROXY set to: $HTTPS_PROXY"
      echo "NO PROXY set to: $NO_PROXY"
    fi

    echo "."
    echo "."
    echo "CODENVY: RUNNING CODENVY INSTALLER"
    echo "."
    echo "."

    if [ -n "$HTTP_PROXY" ] || [ -n "$HTTPS_PROXY" ]; then
      bash <(curl -L -s --proxy ${HTTP_PROXY} ${CODENVY_URL}) ${CODENVY_OPTIONS} \
                                                              --http-proxy-for-installation=${HTTP_PROXY} \
                                                              --https-proxy-for-installation=${HTTPS_PROXY} \
                                                              --no-proxy-for-installation=${NO_PROXY}
    else
      bash <(curl -L -s ${CODENVY_URL}) ${CODENVY_OPTIONS}
    fi

    echo "."
    echo "."
    echo "CODENVY: INSTALLED!"
    echo 'Add "'${IP}' codenvy.onprem" to your hosts file'
    echo 'Access:   http://codenvy.onprem'
    echo 'Username: admin'
    echo 'Password: password'
    echo "."
    echo "."
  SHELL

  config.vm.provision "shell" do |s|
    s.inline = $script
    s.args = [http_proxy, https_proxy, no_proxy, codenvy_url, codenvy_options, ip]
  end

end
