# Biletix Mikroservis Test Rehberi

## 🚀 Başlangıç Adımları

### 1. Kafka ve Redis'i Başlat
```bash
docker-compose up -d
```
- **Kafka UI:** http://localhost:8096 (Kafka mesajlarını görmek için)
- **Redis Commander:** http://localhost:8097 (Redis key'lerini görmek için)

### 2. PostgreSQL Veritabanlarını Hazırla
Aşağıdaki veritabanlarının PostgreSQL'de oluşturulmuş olduğundan emin ol:
- `event` (port: 5432)
- `ticket` (port: 5432)
- `orderTicket` (port: 5432)
- `payment` (port: 5432)
- `notification` (port: 5432)

### 3. Servisleri Sırayla Başlat

**ÖNEMLİ:** Servisleri bu sırayla başlatın:

1. **Eureka Server** (port: 8761)
   - Service Discovery için gerekli
   - URL: http://localhost:8761

2. **Event Service** (port: 8085)
   - Event oluşturma için

3. **Ticket Service** (port: 8086)
   - Bilet yönetimi için
   - Event Service'ten EVENT_CREATED event'ini dinler

4. **Order Service** (port: 8087)
   - Sipariş yönetimi için
   - Ticket Service'e Feign ile bağlanır

5. **Payment Service** (port: 8088)
   - Ödeme işlemleri için
   - Kafka'ya PAYMENT_SUCCESS/FAILED ve ORDER_COMPLETED event'lerini gönderir

6. **Notification Service** (port: 8089)
   - Bildirim gönderme için
   - ORDER_COMPLETED event'ini dinler

---

## 📋 Test Senaryosu

### Senaryo 1: Event Oluşturma ve Bilet Stoku Oluşturma

**Adım 1:** Event oluştur
```bash
POST http://localhost:8085/api/events
Content-Type: application/json

{
  "name": "Tarkan Konseri",
  "description": "Büyük konser",
  "imageUrl": "https://example.com/tarkan.jpg",
  "eventDate": "2025-12-25T20:00:00",
  "doorsOpen": "2025-12-25T19:00:00",
  "salesStartDate": "2025-12-01T10:00:00",
  "salesEndDate": "2025-12-25T18:00:00",
  "venueId": "venue-id-123",
  "attributes": {}
}
```

**Beklenen Sonuç:**
- Event oluşturulur
- Kafka'ya `EVENT_CREATED` event'i gönderilir (eventId, venueId, priceCategories detayları ile)
- Ticket Service bu event'i dinler
- Her PriceCategory için `totalAllocation` kadar bilet stoku oluşturulur (AVAILABLE statüsünde)
- Her bilet: eventId, venueId, sectionId, priceCategoryId, price, currency bilgilerini içerir
- userId başlangıçta null (satıştan sonra atanır)

**Kontrol:**
```bash
GET http://localhost:8086/ticket-stocks/by-event/{eventId}
```
TicketStock kayıtlarının oluşturulduğunu ve `availableCount` değerlerini kontrol et.

---

### Senaryo 2: Sipariş Oluşturma (Yeni Yapı)

**Adım 1:** Mevcut stokları kontrol et
```bash
GET http://localhost:8086/ticket-stocks/by-event/{eventId}
```
StockId'yi not al (örn: `stock-uuid-123`)

**Adım 2:** Order oluştur
```bash
POST http://localhost:8087/api/orders
Content-Type: application/json

{
  "userId": "user-123",
  "stockId": "stock-uuid-123",
  "quantity": 2,
  "seatLabels": ["A-15", "A-16"],
  "idempotencyKey": "order-key-123"
}
```

**Beklenen Sonuç:**
- Order oluşturulur (PENDING statüsünde)
- OrderItem'lar oluşturulur (henüz ticketId yok)

**Kontrol:**
```bash
GET http://localhost:8087/api/orders/{orderId}
```
Order'ın `PENDING` statüsünde ve `stockId`, `quantity` bilgilerinin doğru olduğunu kontrol et.

---

### Senaryo 3: Ödeme Yapma ve Sipariş Tamamlama

**Adım 1:** Payment oluştur
```bash
POST http://localhost:8088/api/payments
Content-Type: application/json

{
  "orderId": "order-id-from-step2",
  "userId": "user-123",
  "amount": 500.00,
  "currency": "TRY",
  "paymentMethod": "CREDIT_CARD",
  "cardNumber": "1234567890123456",
  "cvv": "123",
  "expireDate": "12/25",
  "cardHolderName": "John Doe"
}
```

**Adım 2:** Ödeme başarılı olunca siparişi tamamla (Biletler oluşturulur)
```bash
POST http://localhost:8087/api/orders/{orderId}/complete
```

**Beklenen Sonuç:**
- Order `COMPLETED` statüsüne geçer
- TicketService'ten biletler oluşturulur (`purchaseTickets` API)
- Her bilet için `ticketId`, `qrCode`, `seatLabel` atanır
- TicketStock'ta `availableCount` azalır, `soldCount` artar
- Kafka'ya `ORDER_COMPLETED` event'i gönderilir
- Notification Service bu event'i dinler ve bildirim gönderir

**Kontrol:**
```bash
# Order'ın COMPLETED olduğunu ve ticket bilgilerini kontrol et
GET http://localhost:8087/api/orders/{orderId}

# Ticket'ların oluşturulduğunu kontrol et (items içinde ticketId ve qrCode olmalı)

# TicketStock'un güncellendiğini kontrol et
GET http://localhost:8086/ticket-stocks/by-event/{eventId}

# Kullanıcının biletlerini kontrol et
GET http://localhost:8086/tickets/by-user/user-123
```

---

### Senaryo 4: Sipariş İptal Etme (Sadece PENDING siparişler için)

> ⚠️ **Önemli:** Sadece `PENDING` durumundaki siparişler iptal edilebilir.
> `COMPLETED` durumundaki siparişler için bilet iadesi (Senaryo 6) kullanılmalıdır.

```bash
# Önce PENDING durumunda bir sipariş oluşturun (ödeme yapmadan)
POST http://localhost:8087/api/orders/{pendingOrderId}/cancel?reason=User%20cancelled
```

**Beklenen Sonuç:**
- Order `CANCELLED` statüsüne geçer
- `TicketStock`'taki `lockedCount` azalır, `availableCount` artar
- `ORDER_CANCELLED` event'i Kafka'ya gönderilir

**Hata Durumları:**
| Sipariş Durumu | Sonuç |
|----------------|-------|
| `PENDING` | ✅ İptal edilir |
| `COMPLETED` | ❌ "Cannot cancel a completed order. Use refund instead." hatası |
| `CANCELLED` | ❌ "Order cannot be cancelled in status: CANCELLED" hatası |

---

### Senaryo 5: Bilet Kullanma (QR Taratma)

```bash
POST http://localhost:8086/tickets/{ticketId}/use?usedBy=gate-1
```

**Beklenen Sonuç:**
- Ticket `USED` statüsüne geçer
- TicketHistory'e kayıt eklenir (SOLD → USED)

---

### Senaryo 6: Bilet İade Etme

```bash
POST http://localhost:8086/tickets/{ticketId}/refund?refundedBy=admin-1&reason=Event%20cancelled
```

**Beklenen Sonuç:**
- Ticket `REFUNDED` statüsüne geçer
- TicketStock'ta `availableCount` artar, `soldCount` azalır
- TicketHistory'e kayıt eklenir (SOLD → REFUNDED)

---

### Senaryo 7: Redis Koltuk Kilitleme (Seat Lock)

> 💡 **Yeni Özellik:** Bilet kilitleme artık Redis üzerinde yapılıyor!
> TTL (Time To Live) ile 5 dakika içinde ödeme yapılmazsa kilit otomatik kalkar.

**Adım 1:** Kilitli koltukları kontrol et (Redis Commander ile)
1. Redis Commander'a git: http://localhost:8097
2. `seat:*` pattern'ıyla key'leri ara
3. Key yapısı:
   - `seat:lock:{stockId}:{seatLabel}` → Numaralı koltuklar
   - `seat:generic:{stockId}:{orderId}` → Numarasız koltuklar
   - `seat:total:{stockId}` → Toplam kilitli sayısı

**Adım 2:** Redis üzerinden kilitleri kontrol et (API ile)
```bash
# Bir stok için Redis'teki kilitli bilet sayısı
GET http://localhost:8086/ticket-stocks/{stockId}/redis-locked-count
```

**Test Senaryosu:**

1. **Sipariş oluştur** (koltuklar Redis'te kilitlenir - 5 dk TTL)
   ```bash
   POST http://localhost:8087/api/orders
   {
     "userId": "user-123",
     "stockId": "stock-id",
     "quantity": 2,
     "seatLabels": ["A-15", "A-16"],
     "idempotencyKey": "order-key-xxx"
   }
   ```

2. **Redis Commander'da kontrol et:**
   - `seat:lock:{stockId}:A-15` → `order-id` değeri
   - `seat:lock:{stockId}:A-16` → `order-id` değeri
   - `seat:total:{stockId}` → `2` değeri

3. **5 dakika bekleyin (veya manuel cancel):**
   - Redis TTL dolunca kilitler otomatik kalkar
   - VEYA `POST /api/orders/{orderId}/cancel` ile iptal edin

4. **Tekrar kontrol:**
   - Redis key'leri silinmiş olmalı

**Beklenen Davranış:**
| İşlem | Redis | DB (TicketStock) |
|-------|-------|------------------|
| Order oluştur | Kilitler oluşur (5dk TTL) | `availableCount` ↓, `lockedCount` ↑ |
| Ödeme başarılı | Kilitler kaldırılır | `lockedCount` ↓, `soldCount` ↑ |
| Ödeme başarısız/iptal | Kilitler kaldırılır | `lockedCount` ↓, `availableCount` ↑ |
| 5 dk timeout | TTL ile otomatik silinir | Scheduler düzeltir (TODO) |

---

## 🔍 Debug ve Kontrol

### Redis Kontrolü
1. **Redis Commander:** http://localhost:8097
2. Tüm `seat:*` key'lerini listele
3. TTL değerlerini kontrol et (saniye cinsinden)

### Kafka Mesajlarını İzleme
1. Kafka UI'ya git: http://localhost:8096
2. Topics sekmesine git
3. `event-events` ve `payment-events` topic'lerini kontrol et
4. Mesajları görüntüle

### Eureka Dashboard
- URL: http://localhost:8761
- Tüm servislerin kayıtlı olduğunu kontrol et

### Log Kontrolü
Her servisin console log'larını kontrol et:
- Event Service: `EVENT_CREATED` event'inin gönderildiğini görmeli
- Ticket Service: Event'leri dinlediğini ve ticket'ları güncellediğini görmeli
- Payment Service: `PAYMENT_SUCCESS` ve `ORDER_COMPLETED` event'lerinin gönderildiğini görmeli
- Notification Service: `ORDER_COMPLETED` event'ini dinlediğini ve PDF oluşturduğunu görmeli

---

## ⚠️ Sorun Giderme

### Kafka bağlantı hatası
- `docker-compose ps` ile Kafka'nın çalıştığını kontrol et
- `docker-compose logs kafka` ile log'ları kontrol et

### Feign Client hatası
- Eureka Server'ın çalıştığından emin ol
- Servislerin Eureka'ya kayıtlı olduğunu kontrol et

### Database hatası
- PostgreSQL'in çalıştığından emin ol
- Veritabanlarının oluşturulduğunu kontrol et

### Mail gönderme hatası
- Mail konfigürasyonu yorum satırında olduğu için hata vermemeli
- Mail göndermek istiyorsanız `application.properties`'teki yorum satırlarını kaldırın

---

## 📝 Test Checklist

- [ ] **Infrastructure**
  - [ ] Kafka çalışıyor (`docker-compose up -d`)
  - [ ] Redis çalışıyor (`docker-compose up -d`)
  - [ ] Eureka Server çalışıyor (port: 8761)
  
- [ ] **Servisler**
  - [ ] Tüm servisler başlatıldı ve Eureka'ya kayıtlı
  
- [ ] **Event & Stock Oluşturma**
  - [ ] Event oluşturuldu
  - [ ] Bilet stoku otomatik oluşturuldu (Kafka üzerinden)
  
- [ ] **Sipariş Akışı**
  - [ ] Order oluşturuldu (PENDING status)
  - [ ] Redis'te koltuk kilitleri oluştu (5 dk TTL)
  - [ ] DB'de `availableCount` azaldı, `lockedCount` arttı
  
- [ ] **Ödeme Akışı**
  - [ ] Payment yapıldı
  - [ ] Ticket entity'leri oluşturuldu (SOLD status)
  - [ ] Redis kilitleri kaldırıldı
  - [ ] DB'de `lockedCount` azaldı, `soldCount` arttı
  
- [ ] **Notification**
  - [ ] ORDER_COMPLETED event'i gönderildi
  - [ ] PDF bilet oluşturuldu (log'larda görünmeli)
  - [ ] Mail gönderildi (mail konfigürasyonu varsa)
  
- [ ] **İptal/Timeout Testi**
  - [ ] Order iptal edildi veya 5 dk beklendi
  - [ ] Redis kilitleri temizlendi
  - [ ] DB'de koltuklar tekrar müsait oldu

---

## 🎯 Hızlı Test (Postman/curl)

Tüm senaryoyu tek seferde test etmek için:

```bash
# 1. Event oluştur
curl -X POST http://localhost:8085/api/events \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Event","eventDate":"2025-12-25T20:00:00","venueId":"venue-1"}'

# 2. Biletleri kontrol et (eventId'yi yukarıdaki response'dan al)
curl http://localhost:8086/tickets/by-event/{eventId}

# 3. Order oluştur (ticketId'leri yukarıdaki response'dan al)
curl -X POST http://localhost:8087/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","totalAmount":500,"currency":"TRY","idempotencyKey":"key-1","items":[{"ticketId":"ticket-1","eventId":"event-1","price":250}]}'

# 4. Payment yap (orderId'yi yukarıdaki response'dan al)
curl -X POST http://localhost:8088/api/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":"order-1","userId":"user-1","amount":500,"currency":"TRY","paymentMethod":"CREDIT_CARD","cardNumber":"1234","cvv":"123","expireDate":"12/25","cardHolderName":"Test"}'
```

---

**Not:** Gerçek test için önce Venue ve diğer gerekli entity'leri oluşturmanız gerekebilir. Bu rehber temel akışı test etmek içindir.

