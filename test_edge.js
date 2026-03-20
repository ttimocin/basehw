async function test() {
  const res = await fetch('https://npzgabcblqdbnktruqcz.supabase.co/functions/v1/catalog-sync', {
    headers: {
      'Authorization': 'Bearer ' + process.argv[2]
    }
  });
  console.log(res.status);
  const text = await res.text();
  console.log(text.substring(0, 100)); // only first 100 chars
}
test();
