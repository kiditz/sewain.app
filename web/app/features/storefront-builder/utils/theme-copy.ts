import type {StorefrontBlock} from '~/types/storefront';
const translations:Record<string,string>={
 'Ready for the outdoors':'Siap menjelajah alam','EXPLORE MORE':'JELAJAHI LEBIH JAUH','Gear up for your next escape. Rent what you need and make every adventure count.':'Siapkan perlengkapan untuk perjalanan berikutnya. Sewa sesuai kebutuhan dan nikmati setiap petualangan.',
 'Find your next adventure':'Temukan petualangan berikutnya','Gear for the journey':'Perlengkapan perjalanan','A little room to explore':'Ruang untuk menjelajah','Fresh finds':'Pilihan terbaru','Good days, made simple':'Hari menyenangkan, tanpa repot','Find the things you need for a slower, brighter kind of day.':'Temukan kebutuhan untuk hari yang lebih santai dan menyenangkan.',
 'Objects with a story':'Setiap benda punya cerita','Thoughtfully curated':'Dipilih dengan cermat','Considered essentials':'Kebutuhan yang bermakna','Make every moment bloom':'Rayakan setiap momen','SOMETHING SPECIAL':'UNTUK MOMEN SPESIAL','Thoughtful details for the celebrations and everyday moments that matter.':'Detail istimewa untuk perayaan dan momen sehari-hari yang berarti.','For your special moments':'Untuk momen spesialmu','Explore the possibilities':'Jelajahi berbagai kemungkinan',
 'Only the essentials':'Cukup yang dibutuhkan','LESS, BUT BETTER':'SEDIKIT, LEBIH BERARTI','Useful things. Clear choices. Ready when you are.':'Barang bermanfaat. Pilihan sederhana. Siap saat dibutuhkan.','The selection':'Pilihan kami','Browse collections':'Jelajahi koleksi','Keep it simple':'Nikmati kemudahannya','Rent what you need, for as long as you need it.':'Sewa sesuai kebutuhan, selama yang kamu perlukan.',
 'Standout essentials':'Pilihan yang menonjol','Discover something different':'Temukan sesuatu yang berbeda','A more considered way':'Cara yang lebih bermakna','ROOTED IN SIMPLICITY':'BERAWAL DARI KESEDERHANAAN','Beautiful, useful things to share. Make space for experiences, not possessions.':'Barang indah dan bermanfaat untuk berbagi. Beri ruang untuk pengalaman.','Naturally useful':'Bermanfaat setiap hari','Made for everyday living':'Untuk keseharianmu','Use more. Own less.':'Nikmati lebih banyak. Miliki lebih sedikit.','Choose what works for you today, and leave room for tomorrow.':'Pilih kebutuhanmu hari ini, dan sisakan ruang untuk esok.',
};
const controlTranslations:Record<string,string>={
 'Rental items':'Barang sewaan','Your cart is empty':'Keranjangmu masih kosong',
 "You haven't added any rental items yet. Explore our collection to get started.":'Belum ada barang sewaan di keranjangmu. Jelajahi koleksi untuk memulai.',
 'Explore products':'Jelajahi produk','Continue shopping':'Lanjut belanja','Order summary':'Ringkasan pesanan',
 'Rental subtotal':'Subtotal sewa','Refundable deposit':'Deposit yang dapat dikembalikan','Rental + deposit':'Sewa + deposit',
 "Deposits are subject to the store's rental and return policies.":'Deposit mengikuti ketentuan sewa dan pengembalian toko.',
 'Proceed to checkout':'Lanjut ke checkout','Choose an option':'Pilih varian','Rental duration':'Durasi sewa','Quantity':'Jumlah',
 'Rent now':'Sewa Sekarang','Payment summary':'Ringkasan pembayaran',
};
const controlFields=['title','label','emptyTitle','emptyText','emptyButtonLabel','continueLabel','rentalLabel','depositLabel','totalLabel','totalHint','noteText'];

export function themeCopy(value:string,locale:string){return locale.startsWith('id')?translations[value]??value:value;}
export function localizeThemeBlocks(blocks:StorefrontBlock[],locale:string){
 const id=locale.startsWith('id');
 const copy={heading:id?'Ruang untuk pengalaman baru':'Make room for new experiences',text:id?'Sewa barang pilihan untuk kebutuhan sehari-hari dan momen istimewa.':'Rent considered essentials for everyday needs and special moments.',eyebrow:id?'PILIHAN KAMI':'OUR SELECTION',button:id?'Jelajahi produk':'Explore products'};
 let faqIndex=0,iconIndex=0;
 for(const block of blocks){
  if(block.type==='SLIDE')block.settings.name=`${id?'Visual':'Slide'} ${blocks.indexOf(block)+1}`;
  if(['HEADING','TEXT','EYEBROW','JUMBO_TEXT','MARQUEE_TEXT'].includes(block.type))block.settings.text=block.type==='HEADING'||block.type==='JUMBO_TEXT'?copy.heading:block.type==='EYEBROW'?copy.eyebrow:copy.text;
  if(block.type==='BUTTON')block.settings.label=copy.button;
  if(block.type==='ICON_TEXT'){block.settings.title=id?'Sewa lebih mudah':'Rent with confidence';block.settings.text=id?'Pilihan berkualitas, siap menemani rencanamu.':'Quality essentials, ready for your plans.';}
  if(block.type==='FAQ_ITEM'){
    const questions=id?['Bagaimana cara menyewa?','Bisa memilih durasi sewa?','Bagaimana cara mengembalikan barang?']:['How do I rent?','Can I choose my rental period?','How do I return an item?'];
    const answers=id?['Pilih produk, tentukan tanggal sewa, lalu selesaikan pesananmu.','Pilih jadwal dan paket sewa yang tersedia pada halaman produk.','Ikuti instruksi pengembalian yang diberikan toko setelah pemesanan.']:['Choose a product and your rental dates, then complete your order.','Choose the available dates and rental plan on the product page.','Follow the return instructions provided by the store after booking.'];
    block.settings.question=questions[faqIndex%3];block.settings.answer=answers[faqIndex++%3];
  }
  if(block.type==='ICON_TEXT'){
    const titles=id?['Pilihan berkualitas','Durasi fleksibel','Pesan dengan mudah']:['Considered essentials','Flexible rental periods','Simple booking'];
    block.settings.title=titles[iconIndex++%3];
  }
  for(const key of controlFields){
    const value=block.settings[key];
    if(typeof value!=='string')continue;
    if(id&&controlTranslations[value])block.settings[key]=controlTranslations[value];
    if(!id){const original=Object.entries(controlTranslations).find(([,translation])=>translation===value);if(original)block.settings[key]=original[0];}
  }
  if(block.blocks)localizeThemeBlocks(block.blocks,locale);
 }
}
