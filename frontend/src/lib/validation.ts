type FormControl = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

export const vietnamPhonePattern = '(0|[+]84)[35789][0-9]{8}';

export function vietnameseValidationMessage(field: FormControl): string {
  const v = field.validity;
  if (v.valueMissing)
    return field instanceof HTMLSelectElement
      ? 'Vui lòng chọn một giá trị.'
      : 'Vui lòng điền thông tin này.';
  if (v.typeMismatch)
    return field instanceof HTMLInputElement && field.type === 'email'
      ? 'Vui lòng nhập địa chỉ email hợp lệ, ví dụ: ten@gmail.com.'
      : 'Vui lòng nhập đường dẫn hợp lệ.';
  if (v.patternMismatch) {
    if (field.name === 'phoneNumber' || field.name === 'receiverPhone')
      return 'Vui lòng nhập số điện thoại Việt Nam: 10 số bắt đầu bằng 03, 05, 07, 08, 09 hoặc dùng +84.';
    if (field.name === 'verificationCode') return 'Mã xác minh phải gồm 6 chữ số.';
    if (field.name === 'otp') return 'Mã OTP phải gồm 6 chữ số.';
    return field.title || 'Thông tin không đúng định dạng yêu cầu.';
  }
  if (!(field instanceof HTMLSelectElement)) {
    if (v.tooShort) return `Vui lòng nhập ít nhất ${field.minLength} ký tự.`;
    if (v.tooLong) return `Vui lòng nhập tối đa ${field.maxLength} ký tự.`;
  }
  if (field instanceof HTMLInputElement) {
    if (v.rangeUnderflow) return `Giá trị phải lớn hơn hoặc bằng ${field.min}.`;
    if (v.rangeOverflow) return `Giá trị phải nhỏ hơn hoặc bằng ${field.max}.`;
  }
  if (v.badInput || v.stepMismatch) return 'Vui lòng nhập một số hợp lệ theo bước cho phép.';
  return 'Thông tin chưa hợp lệ. Vui lòng kiểm tra lại.';
}

export function installVietnameseValidation(root: Document = document) {
  const isControl = (target: EventTarget | null): target is FormControl =>
    target instanceof HTMLInputElement ||
    target instanceof HTMLSelectElement ||
    target instanceof HTMLTextAreaElement;
  const invalid = (event: Event) => {
    if (isControl(event.target))
      event.target.setCustomValidity(vietnameseValidationMessage(event.target));
  };
  const clear = (event: Event) => {
    if (isControl(event.target)) event.target.setCustomValidity('');
  };
  root.addEventListener('invalid', invalid, true);
  root.addEventListener('input', clear, true);
  root.addEventListener('change', clear, true);
  return () => {
    root.removeEventListener('invalid', invalid, true);
    root.removeEventListener('input', clear, true);
    root.removeEventListener('change', clear, true);
  };
}
