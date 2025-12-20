# Eureka'dan Nacos'a Geçiş Rehberi

Bu rehber, Biletix mikroservis projesinde Eureka Server'dan Nacos'a geçiş için gerekli tüm adımları içerir.

## 📋 İçindekiler

1. [Nacos Nedir?](#nacos-nedir)
2. [Nacos Kurulumu](#nacos-kurulumu)
3. [Maven Bağımlılıkları](#maven-bağımlılıkları)
4. [Application Class Değişiklikleri](#application-class-değişiklikleri)
5. [Configuration Dosyaları](#configuration-dosyaları)
6. [Eureka Server'ı Kaldırma](#eureka-serverı-kaldırma)
7. [Test Etme](#test-etme)

---

## 🎯 Nacos Nedir?

Nacos (Naming and Configuration Service), Alibaba tarafından geliştirilen bir service discovery ve configuration management aracıdır.

### Eureka'ya Göre Avantajları:
- ✅ Service Discovery + Configuration Management (ikisi birden)
- ✅ Daha iyi performans
- ✅ Daha aktif geliştirme
- ✅ Health check ve load balancing
- ✅ Dynamic configuration management
- ✅ Namespace ve Group desteği

---

## 🚀 Nacos Kurulumu

### Docker ile Kurulum (Önerilen)

```bash
docker run -d \
  --name nacos-server \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  -e PREFER_HOST_MODE=hostname \
  nacos/nacos-server:v2.3.0
```

### Manuel Kurulum

1. Nacos'u indirin: https://github.com/alibaba/nacos/releases
2. Zip dosyasını çıkarın
3. `bin/startup.sh` (Linux/Mac) veya `bin/startup.cmd` (Windows) çalıştırın
4. Tarayıcıda açın: http://localhost:8848/nacos
5. Varsayılan kullanıcı adı/şifre: `nacos/nacos`

---

## 📦 Maven Bağımlılıkları

### Tüm Servislerde Yapılacak Değişiklik

**KALDIRILACAK:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**EKLENECEK:**
```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    <version>2022.0.0.0</version>
</dependency>
```

**NOT:** Spring Cloud Alibaba BOM'unu dependencyManagement'a eklemelisiniz:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Nacos için Spring Cloud Alibaba BOM -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2022.0.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 🔧 Application Class Değişiklikleri

### Eureka Kullanan Tüm Servislerde

**KALDIRILACAK:**
```java
@EnableDiscoveryClient  // Artık gerekli değil, otomatik aktif
```

**DEĞİŞTİRİLECEK:** (Sadece Eureka Server'da)
```java
@EnableEurekaServer  // Bu annotation kaldırılacak, Eureka Server artık kullanılmayacak
```

**NOT:** Nacos için özel bir annotation gerekmez. Spring Boot otomatik olarak Nacos Discovery'yi aktif eder.

---

## ⚙️ Configuration Dosyaları

### application.yml veya application.properties

**KALDIRILACAK (Eureka konfigürasyonu):**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
```

**EKLENECEK (Nacos konfigürasyonu):**
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: public  # Opsiyonel: namespace kullanmak isterseniz
        group: DEFAULT_GROUP  # Opsiyonel: group kullanmak isterseniz
        enabled: true
```

### application.properties Formatında

**KALDIRILACAK:**
```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true
```

**EKLENECEK:**
```properties
spring.cloud.nacos.discovery.server-addr=localhost:8848
spring.cloud.nacos.discovery.namespace=public
spring.cloud.nacos.discovery.group=DEFAULT_GROUP
spring.cloud.nacos.discovery.enabled=true
```

---

## 🗑️ Eureka Server'ı Kaldırma

1. `eurekaServer` klasörünü silebilir veya kullanmamayı tercih edebilirsiniz
2. Docker Compose'da Eureka Server container'ını kaldırın (varsa)
3. Eureka Server'ı çalıştırmayı durdurun

---

## ✅ Test Etme

### 1. Nacos Console'u Kontrol Edin

http://localhost:8848/nacos adresine gidin ve servislerin kayıt olduğunu kontrol edin:
- **Services** → **Service List** menüsünden tüm servisleri görebilirsiniz

### 2. Servisleri Başlatın

Servisleri sırayla başlatın:
1. Nacos Server (öncelikle)
2. Event Service
3. Ticket Service
4. Order Service
5. Payment Service
6. Notification Service
7. Identity Service
8. API Gateway

### 3. API Gateway'i Test Edin

API Gateway'deki `lb://` prefix'li route'lar Nacos ile çalışmalı:
```yaml
uri: lb://ticketService  # Nacos'tan servis adını çözer
```

### 4. Feign Client'ları Test Edin

Feign Client'lar Nacos'tan servis adlarını çözmelidir:
```java
@FeignClient(name = "ticketService")  // Nacos'tan çözülür
```

---

## 📝 Değiştirilecek Dosyalar Listesi

### pom.xml Dosyaları (Tüm Servisler)
- ✅ apiGateway/pom.xml
- ✅ orderService/pom.xml
- ✅ ticketService/pom.xml
- ✅ paymentService/pom.xml
- ✅ notificationService/pom.xml
- ✅ eventService/pom.xml
- ✅ identityService/pom.xml
- ❌ eurekaServer/pom.xml (artık kullanılmayacak)

### Application Class'ları
- ✅ apiGateway/src/main/java/.../ApiGatewayApplication.java
- ✅ orderService/src/main/java/.../OrderServiceApplication.java
- ✅ ticketService/src/main/java/.../TicketServiceApplication.java
- ✅ paymentService/src/main/java/.../PaymentServiceApplication.java
- ✅ notificationService/src/main/java/.../NotificationServiceApplication.java
- ✅ eventService/src/main/java/.../EventServiceApplication.java
- ✅ identityService/src/main/java/.../IdentityServiceApplication.java

### Configuration Dosyaları
- ✅ apiGateway/src/main/resources/application.yml
- ✅ eventService/src/main/resources/application.properties
- ✅ orderService/src/main/resources/application.properties
- ✅ ticketService/src/main/resources/application.properties
- ✅ paymentService/src/main/resources/application.properties
- ✅ notificationService/src/main/resources/application.properties
- ✅ identityService/src/main/resources/application.properties

---

## 🔍 Nacos Console Özellikleri

Nacos Console'da şunları yapabilirsiniz:

1. **Service Management**: Tüm kayıtlı servisleri görüntüleme
2. **Health Check**: Servislerin sağlık durumunu kontrol etme
3. **Configuration Management**: Merkezi konfigürasyon yönetimi
4. **Namespace Management**: Farklı ortamlar için namespace'ler (dev, test, prod)
5. **Service Metadata**: Servis metadata'larını görüntüleme

---

## ⚠️ Önemli Notlar

1. **Spring Cloud Version Uyumluluğu**: 
   - Spring Cloud 2025.0.0 için Nacos 2022.0.0.0 kullanın
   - Farklı versiyonlar için uyumluluk tablosunu kontrol edin

2. **Load Balancer**: 
   - Spring Cloud Load Balancer otomatik olarak Nacos ile çalışır
   - `lb://` prefix'i Nacos'tan servis adını çözer

3. **Feign Client**: 
   - Feign Client'lar Nacos ile otomatik çalışır
   - `@FeignClient(name = "serviceName")` şeklinde kullanılır

4. **Health Check**: 
   - Nacos otomatik health check yapar
   - Sağlıksız servisleri otomatik olarak kaldırır

---

## 🎉 Geçiş Sonrası

Geçiş tamamlandıktan sonra:
- ✅ Tüm servisler Nacos'a kayıt olmalı
- ✅ API Gateway'deki `lb://` route'lar çalışmalı
- ✅ Feign Client'lar servisleri bulabilmeli
- ✅ Nacos Console'da tüm servisler görünmeli

---

## 📚 Ek Kaynaklar

- [Nacos Dokümantasyonu](https://nacos.io/docs/latest/what-is-nacos/)
- [Spring Cloud Alibaba Dokümantasyonu](https://github.com/alibaba/spring-cloud-alibaba)
- [Nacos GitHub](https://github.com/alibaba/nacos)

---

**Son Güncelleme:** 2025-01-XX

