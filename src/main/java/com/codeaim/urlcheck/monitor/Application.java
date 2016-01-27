package com.codeaim.urlcheck.monitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.codeaim.urlcheck.monitor.model.Monitor;
import com.codeaim.urlcheck.monitor.repository.MonitorRepository;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories
public class Application implements CommandLineRunner
{
    @Autowired
    private MonitorRepository monitorRepository;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class);
    }

    @Override
    public void run(String... args) throws Exception {
        monitorRepository.deleteAll();
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Google").url("http://www.google.com").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Facebook").url("http://www.facebook.com").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Yahoo").url("http://www.yahoo.com").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("JSFiddle").url("http://jsfiddle.net/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Pluralsight").url("http://www.pluralsight.com/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("StackOverflow").url("http://stackoverflow.com/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("GitHub").url("https://github.com").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("The Morning Paper").url("http://blog.acolyer.org/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Domainsbot Hypermedia Search").url("http://www.domainsbot.com/d/hypermedia").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Netflix").url("http://www.netflix.com/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Songfacts").url("http://www.songfacts.com/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Codeplex").url("http://www.codeplex.com/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("SEOSiteCheckup").url("http://seositecheckup.com/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("RegexHero").url("http://regexhero.net/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("DigitalOcean").url("https://www.digitalocean.com/join/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Visual Studio Blog").url("http://blogs.msdn.com/b/visualstudio/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Jimmy R").url("http://www.jimmyr.com/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Billboard Article").url("http://www.billboard.com/articles/news/6706919/happy-birthday-song-public-domain-warner-chappel").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Imgur").url("http://imgur.com/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("You Betrayed Us Heroku").url("https://www.youbetrayedus.org/heroku/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("GitLab").url("https://about.gitlab.com/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("BBC News").url("http://www.bbc.co.uk/news").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Sky News").url("http://news.sky.com/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("The Mirror").url("http://www.mirror.co.uk/news/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Telegraph").url("http://www.telegraph.co.uk/news/").interval(1).build());
        monitorRepository.save(Monitor.builder().userId("gdownes").name("Telegraph").url("http://www.hydra-cg.com/spec/latest/core/").interval(1).build());
    }
}
