# ADR-0004: Servis sınırları ve veri sahipliği

- Durum: Kabul edildi
- Tarih: 2026-11-15

## Bağlam

OrderHub şu an tek servisten (product-service) oluşuyor. Yeni ihtiyaçlar:
ürün detayında stok adedini göstermek ve ileride sipariş almak.

Stok adedini `Product`'a bir alan olarak eklemek en kısa yol. Ama stok, üründen
çok farklı bir hızda ve farklı nedenlerle değişir: ürün bilgisi ayda bir
güncellenir, stok her siparişte. Farklı kurallar, farklı yük ve ileride farklı
ekipler demek. Bu yüzden sistemi iş alanlarına (bounded context) göre bölüyoruz.

## Karar

### Bounded context'ler

| Context | Servis | Sorumluluk |
|---|---|---|
| Catalog | product-service | Ürünün ne olduğu: ad, açıklama, fiyat, kategori, aktiflik |
| Inventory | inventory-service | Üründen kaç tane olduğu: kullanılabilir ve rezerve adet |
| Ordering | order-service | Kimin neyi ne fiyata sipariş ettiği, siparişin durumu |

### Veri sahipliği

| Veri | Sahibi | Not |
|---|---|---|
| Ürün adı, açıklama, kategori, fiyat | product-service | Tek doğru kaynak |
| Kullanılabilir / rezerve adet | inventory-service | product-service bu veriyi tutmaz, sorar |
| Sipariş, sipariş satırı | order-service | — |
| Sipariş satırındaki birim fiyat | order-service (**snapshot**) | Sipariş anında product-service'ten alınıp satıra kopyalanır. Fiyat sonradan değişse de geçmiş siparişin tutarı değişmez. İstemci fiyat göndermez. |

Bir verinin tek sahibi vardır; diğer servisler o veriyi sahibinden API ile ister
ya da gerektiğinde kopyasını (snapshot) kendi amacı için saklar.

### Ortak anahtar: SKU

Servisler ürünü **SKU** ile tanır. UUID, product-service'in kendi iç kimliğidir:
onu product-service üretir ve sadece o bilir. SKU ise iş dünyasının anahtarıdır:
depo, sipariş ve katalog aynı SKU'yu kullanır, değişmez (`Product`'ta `final`).

### İletişim

- **Şimdi, senkron (REST + RestClient):** Okuma ihtiyaçları anlık cevap
  gerektirir.
    - product-service → inventory-service: ürün detayında stok adedi (B10-B)
    - order-service → product-service: sipariş anında fiyat, okumada ürün adı (B10-E)
- **İleride, asenkron (Kafka):** Durum değiştiren servisler arası akışlar.
  Sipariş verilince stoğun rezerve edilmesi `OrderPlaced` event'i ile yapılır
  (choreography saga, Hafta 3). Böylece sipariş anında inventory-service'in
  ayakta olması gerekmez.

### Veritabanı: database-per-service

Her servisin kendi veritabanı vardır. Bir servis başka servisin tablosunu
okuyamaz ve yazamaz; veriye sadece o servisin API'si veya event'leri üzerinden
erişilir. Aksi halde tablo yapısı servisler arası gizli bir sözleşmeye dönüşür
ve servisler bağımsız değiştirilemez (distributed monolith).

## Değerlendirilen alternatifler

1. **Stok adedini `Product`'a alan olarak eklemek.** En basit yol. Reddedildi:
   ürün ve stok farklı hızda değişir. Her siparişte ürün kaydı kilitlenir,
   katalog okumaları stok yazmalarıyla yarışır, stok kuralları (rezervasyon,
   negatif olamaz) Catalog modeline karışır.
2. **Servisleri ayırıp tek ortak veritabanı kullanmak.** Geçişi kolaylaştırır.
   Reddedildi: servisler şema üzerinden birbirine bağlanır; bir tablodaki
   değişiklik tüm servisleri birlikte deploy etmeyi gerektirir.
3. **UUID'yi ortak anahtar yapmak.** Reddedildi: diğer servisler ürünü önce
   product-service'e sormadan tanıyamaz; iş tarafının kullandığı kimlik SKU'dur.

## Sonuçlar

**Artılar**
- Her servis bağımsız geliştirilir, test edilir, deploy edilir.
- Stok tarafı yük altında kalırsa sadece inventory-service ölçeklenir.
- Her verinin sahibi net; "bu alanı kim değiştirebilir" sorusunun tek cevabı var.

**Bedeller ve nerede ele alınıyor**
- **Ağ çağrısı ve latency:** Ürün detayı artık iki servise bağlı.
  → B10-C: stok bilgisi opt-in (`?showInventory`), ürün bilgisi cache'lenir.
- **Kısmi hata:** inventory-service kapalıysa ürün detayı ne olacak?
  → B10-B: timeout + tolerans (stok `null`, ürün yine 200).
  → Hafta 3: Resilience4j circuit breaker.
- **Tutarlılık:** Sipariş ve stok rezervasyonu iki ayrı veritabanında; tek
  transaction yok. → Hafta 3: saga + idempotent consumer, eventual consistency.
- **Operasyon yükü:** Üç servis, üç port, üç veritabanı.
  → B10-A: port tablosu; B10-D: ortak parent pom; ileride Docker Compose.