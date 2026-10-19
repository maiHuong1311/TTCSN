import { afterEach, expect, it } from 'vitest';
import { installVietnameseValidation, vietnamPhonePattern } from './validation';

let uninstall: (() => void) | undefined;
afterEach(() => {
  uninstall?.();
  document.body.replaceChildren();
});

it('translates native required and email messages and clears errors while typing', () => {
  uninstall = installVietnameseValidation();
  const input = document.createElement('input');
  input.required = true;
  input.type = 'email';
  document.body.append(input);
  expect(input.reportValidity()).toBe(false);
  expect(input.validationMessage).toBe('Vui lòng điền thông tin này.');
  input.value = 'sai-email';
  input.dispatchEvent(new Event('input', { bubbles: true }));
  input.reportValidity();
  expect(input.validationMessage).toContain('địa chỉ email hợp lệ');
  input.value = 'khach@gmail.com';
  input.dispatchEvent(new Event('input', { bubbles: true }));
  expect(input.checkValidity()).toBe(true);
});

it('accepts Vietnamese mobile numbers and rejects foreign numbers or old prefixes', () => {
  uninstall = installVietnameseValidation();
  const input = document.createElement('input');
  input.name = 'phoneNumber';
  input.pattern = vietnamPhonePattern;
  document.body.append(input);
  for (const phone of [
    '0912345678',
    '+84912345678',
    '0351234567',
    '0581234567',
    '0771234567',
    '0881234567',
  ]) {
    input.value = phone;
    input.dispatchEvent(new Event('input', { bubbles: true }));
    expect(input.checkValidity(), phone).toBe(true);
  }
  for (const phone of ['+12025550123', '0123456789', '09123', '09012345678', 'abcdef1234']) {
    input.value = phone;
    input.dispatchEvent(new Event('input', { bubbles: true }));
    expect(input.reportValidity(), phone).toBe(false);
    expect(input.validationMessage).toContain('điện thoại Việt Nam');
  }
});
