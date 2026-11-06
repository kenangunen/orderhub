# ADR-0001: Mono-repo kullanımı

- Durum: Kabul edildi
- Tarih: 2026-11-06

## Bağlam

OrderHub; product, order ve inventory servisleri ile gateway ve discovery
bileşenlerinden oluşacak bir mikroservis projesi. Projeyi tek kişi geliştiriyor.
Servisler aynı anda değişecek (ör. sipariş akışı order + inventory'yi birlikte
etkiler) ve portföy olarak tek bir linkten incelenebilir olması gerekiyor.

## Karar

Tüm servisler tek bir Git reposunda, her biri kendi klasöründe bağımsız
bir Maven projesi olarak tutulacak (`orderhub/product-service`, `orderhub/order-service` ...).
Servisler kod paylaşmaz; birbirleriyle sadece API (HTTP/Kafka) üzerinden konuşur.

## Alternatifler

- **Poly-repo (her servis ayrı repo):** Servis sahipliği ve bağımsız versiyonlama
  net olur; büyük ve birden fazla takımın çalıştığı yapılarda tercih edilir.
  Tek geliştirici için ise birden fazla servisi etkileyen değişiklik birden
  fazla PR'a bölünür, CI/README/Docker Compose ayarları çoğalır.
- **Maven multi-module (parent pom altında modüller):** Ortak bağımlılık yönetimi
  kolaylaşır, ancak servisler build seviyesinde birbirine bağlanır ve
  "bağımsız deploy edilebilir servis" fikrini zayıflatır.

## Sonuçlar

**Artılar**
- Birden fazla servisi etkileyen değişiklik tek commit/PR ile yapılır.
- Tek README, tek Docker Compose, tek CI ile tüm sistem ayağa kalkar.
- Portföy olarak tek linkten incelenebilir.

**Eksiler / riskler**
- Servisler arası sınırlar disiplinle korunmalı; bir servisin diğerinin
  kodunu import etmesi mümkün ama yasak.
- CI'da her commit'te tüm servisleri build etmemek için path bazlı tetikleme gerekir.
- Proje büyüyüp birden fazla takıma dağılırsa poly-repo'ya geçiş düşünülmeli.
