Vagrant.configure("2") do |config|

    config.vm.define "linux" do |linux|
        linux.vm.box = "ubuntu/groovy64"
        linux.vm.synced_folder ".", "/home/vagrant/sandpolis"

        linux.vm.provider "virtualbox" do |virtualbox|
            virtualbox.memory = 8192
            virtualbox.cpus = 16
        end

        # Configure environment
        linux.vm.provision :shell, :inline => "touch .hushlogin"
        linux.vm.provision :shell, :inline => "hostnamectl set-hostname sandpolis_linux && locale-gen en_US.UTF.8"
        linux.vm.provision :shell, :inline => "apt-get update --fix-missing"
        linux.vm.provision :shell, :inline => "apt-get install -q -y g++ make git curl vim libncursesw5-dev"
        linux.vm.provision :shell, :inline => "wget -q -O- https://github.com/AdoptOpenJDK/openjdk15-binaries/releases/download/jdk-15.0.1+9/OpenJDK15U-jdk_x64_linux_hotspot_15.0.1_9.tar.gz | tar zxf -"
        linux.vm.provision :shell, :inline => "echo 'export JAVA_HOME=/home/vagrant/jdk-15.0.1+9' >>/home/vagrant/.profile"
    end

    config.vm.define "windows" do |windows|
        windows.vm.box = "gusztavvargadr/windows-10"
        windows.vm.synced_folder ".", "C:\\Users\\vagrant\\sandpolis"

        windows.vm.provider "virtualbox" do |virtualbox|
            virtualbox.memory = 8192
            virtualbox.cpus = 16
        end

        # Configure environment
        windows.vm.provision :shell, :inline => "choco install -y adoptopenjdk"
    end
end
