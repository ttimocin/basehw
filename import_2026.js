const fs = require('fs');
const { createClient } = require('@supabase/supabase-js');

async function importData() {
    const filePath = 'C:/Users/ttimo/Downloads/hotwheels_2026_cleaned.json';
    const supabaseUrl = process.env.SUPABASE_URL;
    const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

    if (!supabaseUrl || !supabaseKey) {
        console.error('SUPABASE_URL ve SUPABASE_SERVICE_ROLE_KEY environment değişkenleri tanımlanmalı.');
        process.exit(1);
    }

    const supabase = createClient(supabaseUrl, supabaseKey);
    const rawData = fs.readFileSync(filePath, 'utf8');
    const cars = JSON.parse(rawData);

    console.log(`${cars.length} araç okunuyor...`);

    // Filter out duplicates in the same batch to avoid ON CONFLICT error
    const uniqueCarsMap = new Map();
    cars.forEach(car => {
        const key = `${car.model_name}|${car.year}|hotwheels_2026`;
        if (!uniqueCarsMap.has(key)) {
            uniqueCarsMap.set(key, {
                model_name: car.model_name || '',
                series: car.series || '',
                series_num: car.series_num || '',
                year: car.year ? car.year.toString() : null,
                color: car.color || '',
                image_url: car.image_url || '',
                scale: car.scale || '1:64',
                toy_num: car.toy_num || '',
                feature: car.feature || null,
                data_source: 'hotwheels_2026'
            });
        }
    });

    const uniqueCarsList = Array.from(uniqueCarsMap.values());
    console.log(`${uniqueCarsList.length} benzersiz araç yükleniyor...`);

    const batchSize = 100;
    for (let i = 0; i < uniqueCarsList.length; i += batchSize) {
        const batch = uniqueCarsList.slice(i, i + batchSize);

        const { error } = await supabase
            .from('catalog_hot_wheels')
            .upsert(batch, { onConflict: 'model_name,year,data_source' });

        if (error) {
            console.error(`Batch ${i} yüklenirken hata:`, error);
        } else {
            console.log(`${i + batch.length}/${uniqueCarsList.length} araç yüklendi.`);
        }
    }
    console.log('İşlem tamamlandı.');
}

importData();
