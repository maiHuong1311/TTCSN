from typing import Literal
from pydantic import BaseModel, Field, ConfigDict, StrictInt


class Variant(BaseModel):
    size: str = ""
    color: str = ""
    stockQuantity: int = Field(ge=0)


class Product(BaseModel):
    model_config = ConfigDict(extra="ignore")
    productId: int = Field(gt=0)
    categoryId: int
    productName: str = Field(max_length=255)
    description: str | None = Field(default="", max_length=20000)
    material: str | None = ""
    brand: str | None = ""
    gender: str = "Female"
    price: float = Field(ge=0, le=9999999999.99)
    salePrice: float | None = Field(default=None, ge=0)
    status: str = "Active"
    variants: list[Variant] = Field(default_factory=list, max_length=100)

    @property
    def effective_price(self):
        return self.salePrice if self.salePrice is not None else self.price

    @property
    def available(self):
        return self.status == "Active" and any(v.stockQuantity > 0 for v in self.variants)


class AdviseRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    catalog: list[Product] = Field(max_length=2000)


class Plan(BaseModel):
    occasion: Literal["work", "wedding", "summer", "casual", "general"] = "general"
    productIds: list[StrictInt] = Field(default_factory=list, max_length=8)


class Selection(BaseModel):
    productIds: list[int]
    suggestedSize: None = None
    occasion: str
    mode: str
    maxPrice: float | None = None
